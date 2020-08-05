package com.range.mail.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.range.common.constant.OrderConstant;
import com.range.common.exception.NoStockException;
import com.range.common.to.SkuHasStockVo;
import com.range.common.utils.R;
import com.range.common.vo.MemberResponseVo;
import com.range.mail.order.entity.OrderItemEntity;
import com.range.mail.order.enume.OrderStatusEnum;
import com.range.mail.order.feign.CartFeignService;
import com.range.mail.order.feign.MemberFeignService;
import com.range.mail.order.feign.ProductFeignService;
import com.range.mail.order.feign.WareFeignService;
import com.range.mail.order.interceptor.LoginUserInterceptor;
import com.range.mail.order.service.OrderItemService;
import com.range.mail.order.to.OrderCreateTo;
import com.range.mail.order.vo.*;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.range.common.utils.PageUtils;
import com.range.common.utils.Query;

import com.range.mail.order.dao.OrderDao;
import com.range.mail.order.entity.OrderEntity;
import com.range.mail.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {



    private final ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Autowired
    MemberFeignService memberFeignService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    CartFeignService cartFeignService;

    @Autowired
    ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    OrderService orderService;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    OrderItemService orderItemService;

    @Autowired
    ProductFeignService productFeignService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {

        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();

        /**
         * 获取登录用户
         */
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();

        //由于下面变成了异步任务，导致他们新起的线程无法共享到cookie
        //feign的新起请求通过mallFeignConfig给feign新起的请求同步了原先的cookie
        //但是下面的线程不能够同步到请求线程的cookie
        //所以需要先获取再塞入cookie，才能登录认证通过获取数据
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();


        //1、异步任务
        CompletableFuture<Void> getAddressedTask = CompletableFuture.runAsync(() -> {
            // 1 远程查询会员的所有列表

            //在这个线程中共享到原先请求的cookie
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> addresses = memberFeignService.getAddresses(memberResponseVo.getId());
            orderConfirmVo.setAddresses(addresses);
        }, threadPoolExecutor);

        //异步任务
        CompletableFuture<Void> getCartItemsTask = CompletableFuture.runAsync(() -> {
            // 2 远程查询购物车所有选中的购物项

            //在这个线程中共享到原先请求的cookie
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();
            orderConfirmVo.setItems(currentUserCartItems);
        }, threadPoolExecutor).thenRunAsync(() -> {

            //查询商品的库存信息
            List<OrderItemVo> items = orderConfirmVo.getItems();
            List<Long> collect = items.stream().map(o -> o.getSkuId()).collect(Collectors.toList());
            R hasStock = wareFeignService.getSkusHasStock(collect);
            List<SkuHasStockVo> data = (List<SkuHasStockVo>) hasStock.getData(new TypeReference<List<SkuHasStockVo>>() {});
            if (!CollectionUtils.isEmpty(data)) {
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
                orderConfirmVo.setStocks(map);
            }
        }, threadPoolExecutor);

        // 3 查询用户积分
        orderConfirmVo.setIntegration(memberResponseVo.getIntegration());

        // 其他数据在 Bean 中自动计算

        // 防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");

        //将令牌存储在redis中，下单时与页面传过来的令牌进行对比
        stringRedisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId(), token, 30, TimeUnit.MINUTES);

        //将生成的令牌传送给页面
        orderConfirmVo.setOrderToken(token);

        //等待任务完成
        CompletableFuture.allOf(getAddressedTask, getCartItemsTask).get();
        return orderConfirmVo;
    }
    //下单是高并发场景，并不适合使用seata
    @GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo submitVo) {
        //1、下单
        //2、创建订单
        //3、验证令牌【令牌的对比与删除都必须保证原子性】
        //4、验价格
        //5、锁库存
        confirmVoThreadLocal.set(submitVo);
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();

        //获取用户
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        responseVo.setCode(0);

//        这个令牌对比无法保证原子性
//        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
//        String orderToken = submitVo.getOrderToken();
//        String redisToken = stringRedisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId());

        //使用lua脚本保证redis上的原子操作
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        //获取页面的token
        String orderToken = submitVo.getOrderToken();
        // 原子性操作验证和删除令牌
        Long result = stringRedisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
                Collections.singletonList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()), orderToken);
        if (result == 0) {
            // 令牌验证失败
            responseVo.setCode(1);
            return responseVo;
        } else {
            // 验证成功 下单 去创建订单 验证令牌 核算价格 锁定库存
            OrderCreateTo orderCreateTo = createOrder();
            // 验价
            BigDecimal payAmount = orderCreateTo.getOrderEntity().getPayAmount();
            BigDecimal payPrice = submitVo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                // 金额对比
                // 保存订单
                saveOrder(orderCreateTo);
                // 锁定库存 只要有异常就回滚数据
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(orderCreateTo.getOrderEntity().getOrderSn());

                //需要锁定的订单项数据
                List<OrderItemVo> locks = orderCreateTo.getOrderItems().stream().map(o -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(o.getSkuId());
                    itemVo.setCount(o.getSkuQuantity());
                    itemVo.setTitle(o.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());

                lockVo.setLocks(locks);

                //远程锁定库存
                R r = wareFeignService.lockOrder(lockVo);
                if (r.getCode() == 0) {
                    // 锁定成功
                    responseVo.setOrderEntity(orderCreateTo.getOrderEntity());
//                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", orderCreateTo.getOrderEntity());
                    return responseVo;
                } else {
                    // 锁定失败
                    throw new NoStockException((String) r.get("msg"));
                }
            } else {
                responseVo.setCode(2);
                return  responseVo;
            }
        }
    }

    /**
     * 保存订单数据
     * @param orderCreateTO
     */
    private void saveOrder(OrderCreateTo orderCreateTO) {
        OrderEntity orderEntity = orderCreateTO.getOrderEntity();
        orderEntity.setModifyTime(new Date());
        //保存到数据库
        this.save(orderEntity);
        List<OrderItemEntity> orderItems = orderCreateTO.getOrderItems();
        //批量保存
        orderItemService.saveBatch(orderItems);
    }

    //创建订单
    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreateTO = new OrderCreateTo();

        //生成订单号
        String orderSn = IdWorker.getTimeId();

        //创建订单号
        OrderEntity orderEntity = buildOrder(orderSn);
        orderCreateTO.setOrderEntity(orderEntity);
        // 获取所有的订单项
        List<OrderItemEntity> orderItemEntities = buildOrderItems(orderSn);
        orderCreateTO.setOrderItems(orderItemEntities);
        // 计算所有的价格 积分
        computePrice(orderEntity, orderItemEntities);
        return orderCreateTO;
    }


    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");

        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growth = new BigDecimal("0.0");
        // 订单的总额，叠加每一个订单项的总额信息。
        for (OrderItemEntity entity : orderItemEntities) {
            coupon = coupon.add(entity.getCouponAmount());
            integration = integration.add(entity.getIntegrationAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            total = total.add(entity.getRealAmount());

            gift = gift.add(new BigDecimal(entity.getGiftIntegration().toString()));
            growth = growth.add(new BigDecimal(entity.getGiftGrowth().toString()));
        }
        // 订单价格相关
        orderEntity.setTotalAmount(total);
        // 应付金额
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(integration);
        orderEntity.setCouponAmount(coupon);

        // 设置积分信息
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setGrowth(growth.intValue());

        orderEntity.setDeleteStatus(0);
    }


    /**
     * 构建所有订单项数据
     * @return
     * @param orderSn
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //查到购物车所有的数据
        List<OrderItemVo> currentUserCartItems = cartFeignService.getCurrentUserCartItems();

        if (!CollectionUtils.isEmpty(currentUserCartItems)) {
            List<OrderItemEntity> itemEntities = currentUserCartItems.stream().map(cartItem -> {

                //构建每一个订单项
                OrderItemEntity orderItemEntity = buildOrderItem(cartItem);
                orderItemEntity.setOrderSn(orderSn);
                return orderItemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    /**
     * 构建每一个订单项数据
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        // 1 订单信息 订单号
        // 2 SPU信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = (SpuInfoVo) r.getData(new TypeReference<SpuInfoVo>() {});
        orderItemEntity.setSpuId(data.getId());
        orderItemEntity.setSpuBrand(data.getBrandId().toString());
        orderItemEntity.setSpuName(data.getSpuName());
        orderItemEntity.setCategoryId(data.getCatalogId());

        // 3 SKU信息
        orderItemEntity.setSkuId(cartItem.getSkuId());
        orderItemEntity.setSkuName(cartItem.getTitle());
        orderItemEntity.setSkuPic(cartItem.getImage());
        orderItemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttrs = StringUtils.collectionToDelimitedString(cartItem.getSkuAttrs(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttrs);
        orderItemEntity.setSkuQuantity(cartItem.getCount());
        // 4 优惠信息 [不做]

        // 5 积分信息
        orderItemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        orderItemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        // 6 订单的价格信息
        orderItemEntity.setPromotionAmount(new BigDecimal("0"));
        orderItemEntity.setIntegrationAmount(new BigDecimal("0"));
        orderItemEntity.setCouponAmount(new BigDecimal("0"));
        // 当前订单项的实际金额
        BigDecimal origin = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        // 总额减去各种优惠后的价格
        BigDecimal subtract = origin.subtract(orderItemEntity.getCouponAmount()).subtract(orderItemEntity.getIntegrationAmount()).subtract(orderItemEntity.getPromotionAmount());
        orderItemEntity.setRealAmount(subtract);
        return orderItemEntity;
    }


    private OrderEntity buildOrder(String orderSn) {
        MemberResponseVo memberResponseVO = LoginUserInterceptor.loginUser.get();
        OrderEntity orderEntity = new OrderEntity();
        // 创建订单号
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberId(memberResponseVO.getId());

        OrderSubmitVo orderSubmitVO = confirmVoThreadLocal.get();

        // 获取收货地址信息
        R fare = wareFeignService.getFare(orderSubmitVO.getAddrId());
        FareVo fareResponse = (FareVo) fare.getData(new TypeReference<FareVo>() {});

        // 设置运费信息
        orderEntity.setFreightAmount(fareResponse.getFare());
        // 设置收货人信息
        orderEntity.setReceiverCity(fareResponse.getMemberAddressVo().getCity());
        orderEntity.setReceiverDetailAddress(fareResponse.getMemberAddressVo().getDetailAddress());
        orderEntity.setReceiverName(fareResponse.getMemberAddressVo().getName());
        orderEntity.setReceiverPhone(fareResponse.getMemberAddressVo().getPhone());
        orderEntity.setReceiverPostCode(fareResponse.getMemberAddressVo().getPostCode());
        orderEntity.setReceiverProvince(fareResponse.getMemberAddressVo().getProvince());
        orderEntity.setReceiverRegion(fareResponse.getMemberAddressVo().getRegion());

        // 设置订单的状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);


        return orderEntity;
    }

}