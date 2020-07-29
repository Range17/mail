package com.range.mail.mailcart.serivce.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.range.common.utils.R;
import com.range.mail.mailcart.feign.ProductFeign;
import com.range.mail.mailcart.interceptor.CartInterceptor;
import com.range.mail.mailcart.serivce.CartService;
import com.range.mail.mailcart.to.UserInfoTo;
import com.range.mail.mailcart.vo.Cart;
import com.range.mail.mailcart.vo.CartItem;
import com.range.mail.mailcart.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    ProductFeign productFeign;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    private final String CART_PREFIX = "mall:cart:";


    /**
     * 将购物车信息放到redis
     * @param skuId
     * @param num
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {

        //获取购物车信息
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String res = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(res)) {
            CartItem cartItem = new CartItem();
            // 购物车无此商品

            //runAsync()是异步任务，threadPoolExcutor是线程
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                // 1 远程调用查询商品信息
                R info = productFeign.info(skuId);
                SkuInfoVo data = (SkuInfoVo) info.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                });
                // 2 将新商品添加到购物车
                cartItem.setChecked(true);
                cartItem.setCount(num);
                cartItem.setImage(data.getSkuDefaultImg());
                cartItem.setTitle(data.getSkuTitle());
                cartItem.setSkuId(skuId);
                cartItem.setPrice(data.getPrice());
            }, threadPoolExecutor);
            // 3 远程查询 sku 的属性信息
            CompletableFuture<Void> getSkuSaleAttrValuesTask = CompletableFuture.runAsync(() -> {
                List<String> skuSaleAttrValues = productFeign.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttrs(skuSaleAttrValues);
            }, threadPoolExecutor);
            CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrValuesTask).get();

            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        } else {
            CartItem cartItem = JSON.parseObject(res, CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        }
    }

    @Override
    public CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String res = (String) cartOps.get(skuId.toString());
        return JSON.parseObject(res, CartItem.class);
    }

    /**
     * 获取要操作的购物车
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTO = CartInterceptor.threadLocal.get();
        String cartKey = !ObjectUtils.isEmpty(userInfoTO.getUserId()) ? CART_PREFIX + userInfoTO.getUserId() : CART_PREFIX + userInfoTO.getUserKey();
        return stringRedisTemplate.boundHashOps(cartKey);
    }

    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        UserInfoTo userInfoTO = CartInterceptor.threadLocal.get();
        Cart cart = new Cart();
        if (!ObjectUtils.isEmpty(userInfoTO.getUserId())) {
            String cartKey = CART_PREFIX + userInfoTO.getUserId();
            String tempCartKey = CART_PREFIX + userInfoTO.getUserKey();
            // 已登录
            List<CartItem> tempCartItems = getCartItems(tempCartKey);
            if (!CollectionUtils.isEmpty(tempCartItems)) {
                // 如果临时购物车中的数据还未进行合并
                for (CartItem cartItem : tempCartItems) {
                    addToCart(cartItem.getSkuId(), cartItem.getCount());
                }
                // 添加完后删除临时购物车
                clearCart(tempCartKey);
            }
            cart.setItems(getCartItems(cartKey));
        } else {
            // 未登录
            // 获取临时购物车中的所有购物项
            String cartKey = CART_PREFIX + userInfoTO.getUserKey();
            cart.setItems(getCartItems(cartKey));
        }
        return cart;
    }

    @Override
    public void clearCart(String cartKey) {
        stringRedisTemplate.delete(cartKey);
    }

    @Override
    public void checkItem(Long skuId, Integer checked) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartItem = getCartItem(skuId);
        cartItem.setChecked(checked == 1);
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }


    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public void countItem(Long skuId, Integer num) {
        CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }


    private List<CartItem> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if (!CollectionUtils.isEmpty(values)) {
            return values.stream().map(o -> JSON.parseObject((String) o, CartItem.class)).collect(Collectors.toList());
        }
        return null;
    }


}
