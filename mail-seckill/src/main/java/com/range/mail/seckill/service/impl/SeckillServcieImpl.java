package com.range.mail.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.range.common.to.mq.SeckillOrderTo;
import com.range.common.utils.R;
import com.range.common.vo.MemberResponseVo;
import com.range.mail.seckill.feign.CouponFeignService;
import com.range.mail.seckill.feign.ProductFeignService;
import com.range.mail.seckill.interceptor.LoginUserInterceptor;
import com.range.mail.seckill.service.SeckillService;
import com.range.mail.seckill.to.SeckillSkuRedisTo;
import com.range.mail.seckill.vo.SeckillSessionWithSkus;
import com.range.mail.seckill.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SeckillServcieImpl implements SeckillService {


    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RabbitTemplate rabbitTemplate;


    private final String SESSION_CACHE_PREFIX = "seckill:session:";

    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    @Override
    public void uploadSeckillSkuLatest3Days() {
        // 扫描最近三天需要参与秒杀的活动
        R session = couponFeignService.getLatest3DaySession();
        if (session.getCode() == 0) {
            // 查询上架商品
            List<SeckillSessionWithSkus> sessionData = (List<SeckillSessionWithSkus>) session.getData(new TypeReference<List<SeckillSessionWithSkus>>() {
            });
            // 缓存到 Redis
            if (!CollectionUtils.isEmpty(sessionData)) {
                // 1. 缓存活动信息
                saveSessionInfo(sessionData);
                // 2. 缓存活动的关联商品信息
                saveSessionSkuInfo(sessionData);
            }
        }
    }


    //    @SentinelResource(value = "getCurrentSeckillSkus", blockHandler = "handleGetCurrentSeckillSkus")
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        // 1 确定当前时间属于哪个秒杀场次
        long time = new Date().getTime();
        // 获取以 前缀开头的所有key
        Set<String> keys = stringRedisTemplate.keys(SESSION_CACHE_PREFIX + "*");
//        try (Entry entry = SphU.entry("seckillSkus")) {
        for (String key : keys) {
            //  key:  seckill:session:16722xxxxx
            String newKey = key.replace(SESSION_CACHE_PREFIX, "");
            String[] timeRange = newKey.split("_");
            long start = Long.parseLong(timeRange[0]);
            long end = Long.parseLong(timeRange[1]);
            if (time >= start && time <= end) {
                // 获取这个秒杀场次的所有商品信息
                List<String> range = stringRedisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                assert range != null;
                List<String> strings = hashOps.multiGet(range);
                if (!CollectionUtils.isEmpty(strings)) {
                    return strings.stream().map(item -> JSON.parseObject(item, SeckillSkuRedisTo.class))
                            .collect(Collectors.toList());
                }
                break;
            }
        }
//        } catch (BlockException e) {
//            log.info("sentinel回调...", e);
//        }
        // 2 获取这个秒杀场次需要的所有商品信息
        return null;
    }

    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        // 找到所有需要参与秒杀的商品的 key
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (!CollectionUtils.isEmpty(keys)) {
            String regex = "\\d_" + skuId;
            for (String key : keys) {
                if (Pattern.matches(regex, key)) {
                    String json = hashOps.get(key);
                    SeckillSkuRedisTo skuRedisTO = JSON.parseObject(json, SeckillSkuRedisTo.class);

                    // 随机码
                    long current = new Date().getTime();
                    assert skuRedisTO != null;
                    if (current >= skuRedisTO.getStartTime() && current <= skuRedisTO.getEndTime()) {
                        // 如果在秒杀时间内 随机码是必要的 所以不作任何处理
                    } else {
                        skuRedisTO.setRandomCode(null);
                    }
                    return skuRedisTO;
                }
            }
        }
        return null;
    }

    /**
     * TODO 上架秒杀商品时 每一个数据都应该设置过期时间
     * 秒杀后续的流程 简化了收获地址等信息
     *
     * @param killId
     * @param key
     * @param num
     * @return
     */
    @Override
    public String kill(String killId, String key, Integer num) {
        MemberResponseVo member = LoginUserInterceptor.loginUser.get();
        // 获取当前秒杀商品的详细信息
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

        //获取数据
        String json = hashOps.get(killId);
        if (StringUtils.isEmpty(json)) {
            return "";
        } else {
            //执行秒杀

            //redis中的商品数据
            SeckillSkuRedisTo redisTO = JSON.parseObject(json, SeckillSkuRedisTo.class);

            // 校验随机码与商品ID
            Long startTime = redisTO.getStartTime();
            Long endTime = redisTO.getEndTime();
            long ttl = endTime - startTime;
            long time = new Date().getTime();

            //校验时间的合法性（在redis活动上架加上过期时间）
            if (time >= startTime && time <= endTime) {
                // 如果在秒杀时间内

                //redis的随机码
                String randomCode = redisTO.getRandomCode();

                String skuId = redisTO.getPromotionSessionId() + "_" + redisTO.getSkuId();
                if (randomCode.equals(key) && killId.equals(skuId)) {
                    // 验证秒杀数量是否合理
                    if (num <= redisTO.getSeckillLimit()) {
                        // 验证当前用户是否已参与过秒杀，只要秒杀成功就去 redis 占位 数据格式 userId_sessionId_skuId
                        // 通过key可以得知该用户是否参与过
                        String rediskey = member.getId() + "_" + skuId;

                        // setnx 不存在则占位，自动过期功能在秒杀时间 ttl
                        Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(rediskey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                        if (aBoolean) {

                            // 占位成功说明之前未参与过秒杀活动
                            // 引入分布式信号量
                            RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + randomCode);

                            boolean b = semaphore.tryAcquire(num);
                            //秒杀成功
                            //快读下单，发送MQ消息
                            if (b) {
                                // 秒杀成功 快速下单
                                String timeId = IdWorker.getTimeId();

                                //发送消息到 MQ 整个操作时间在 10ms 左右
                                SeckillOrderTo SeckillOrderTo = new SeckillOrderTo();
                                SeckillOrderTo.setOrderSn(timeId);
                                SeckillOrderTo.setMemberId(member.getId());
                                SeckillOrderTo.setNum(num);
                                SeckillOrderTo.setPromotionSessionId(redisTO.getPromotionSessionId());
                                SeckillOrderTo.setSkuId(redisTO.getSkuId());
                                SeckillOrderTo.setSeckillPrice(redisTO.getSeckillPrice());
                                rabbitTemplate.convertAndSend("order-event-exchange", "order.seckill.queue", SeckillOrderTo);
                                return timeId;
                            }
                            return "";
                        } else {
                            //已经购买过
                            return null;
                        }
                    }
                } else {
                    return "";
                }
            } else {
                // 秒杀时间已过
                return "";
            }
        }
        return "";
    }


    /**
     * 缓存活动信息
     *
     * @param sessionData 商品信息
     */
    private void saveSessionInfo(List<SeckillSessionWithSkus> sessionData) {
        sessionData.forEach(session -> {
            //开始结束时间
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();

            //redis中的key
            String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;

            //判断是否存在key
            Boolean hasKey = stringRedisTemplate.hasKey(key);

            if (!hasKey) {
                //获取商品id
                List<String> collect = session.getRelationSkus().stream()
                        .map(item -> item.getPromotionSessionId() + "_" + item.getSkuId().toString()).collect(Collectors.toList());

                //将数据放入redis中
                stringRedisTemplate.opsForList().leftPushAll(key, collect);
            }
        });
    }

    /**
     * 缓存活动的关联信息
     *
     * @param sessionData 商品信息
     */
    private void saveSessionSkuInfo(List<SeckillSessionWithSkus> sessionData) {
        sessionData.forEach(session -> {
            // 准备哈希操作
            BoundHashOperations<String, Object, Object> ops = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

            //缓存商品
            session.getRelationSkus().forEach(seckillSkuVO -> {

                //判断是否有商品信息
                if (!ops.hasKey(seckillSkuVO.getPromotionSessionId().toString() + "_" + seckillSkuVO.getSkuId().toString())) {
                    // 缓存商品
                    SeckillSkuRedisTo redisTO = new SeckillSkuRedisTo();
                    //1、 SKU 的基本数据
                    R skuInfo = productFeignService.getSkuInfo(seckillSkuVO.getSkuId());
                    if (skuInfo.getCode() == 0) {
                        SkuInfoVo info = (SkuInfoVo) skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTO.setSkuInfo(info);
                    }

                    // 2、SKU 的秒杀信息
                    BeanUtils.copyProperties(seckillSkuVO, redisTO);

                    // 3、设置当前商品的秒杀时间信息
                    redisTO.setStartTime(session.getStartTime().getTime());
                    redisTO.setEndTime(session.getEndTime().getTime());

                    // 4、引入随机码
                    String token = UUID.randomUUID().toString().replace("-", "");
                    redisTO.setRandomCode(token);

                    // 保存到 redis
                    String redisJSONString = JSON.toJSONString(seckillSkuVO);
                    ops.put(seckillSkuVO.getPromotionSessionId() + "_" + seckillSkuVO.getSkuId(), redisJSONString);

                    // 使用库存作为分布式信号量
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    // 商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(seckillSkuVO.getSeckillCount());
                }
            });
        });
    }
}
