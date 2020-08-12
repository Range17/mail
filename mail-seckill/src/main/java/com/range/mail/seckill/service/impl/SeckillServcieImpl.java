package com.range.mail.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.range.common.utils.R;
import com.range.mail.seckill.feign.CouponFeignService;
import com.range.mail.seckill.feign.ProductFeignService;
import com.range.mail.seckill.service.SeckillService;
import com.range.mail.seckill.to.SeckillSkuRedisTo;
import com.range.mail.seckill.vo.SeckillSessionWithSkus;
import com.range.mail.seckill.vo.SkuInfoVo;
import io.lettuce.core.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.UUID;
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


    private final String SESSION_CACHE_PREFIX = "seckill:session:";

    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    @Override
    public void uploadSeckillSkuLatest3Days() {
        // 扫描最近三天需要参与秒杀的活动
        R session = couponFeignService.getLatest3DaySession();
        if (session.getCode() == 0) {
            // 查询上架商品
            List<SeckillSessionWithSkus> sessionData = (List<SeckillSessionWithSkus>) session.getData(new TypeReference<List<SeckillSessionWithSkus>>() {});
            // 缓存到 Redis
            if (!CollectionUtils.isEmpty(sessionData)) {
                // 1. 缓存活动信息
                saveSessionInfo(sessionData);
                // 2. 缓存活动的关联商品信息
                saveSessionSkuInfo(sessionData);
            }
        }
    }

    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        return null;
    }

    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        return null;
    }

    @Override
    public String kill(String killId, String key, Integer num) {
        return null;
    }


    /**
     * 缓存活动信息
     * @param sessionData 商品信息
     */
    private void saveSessionInfo(List<SeckillSessionWithSkus> sessionData) {
        sessionData.forEach(session -> {
            //开始结束时间
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();

            //redis中的key
            String key = SESSION_CACHE_PREFIX + startTime + "_" + endTime;
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
     * @param sessionData 商品信息
     */
    private void saveSessionSkuInfo(List<SeckillSessionWithSkus> sessionData) {
        sessionData.forEach(session -> {
            // 准备哈希操作
            BoundHashOperations<String, Object, Object> ops = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

            //缓存商品
            session.getRelationSkus().forEach(seckillSkuVO -> {

                if (!ops.hasKey(seckillSkuVO.getPromotionSessionId().toString() + "_" + seckillSkuVO.getSkuId().toString())) {
                    // 缓存商品
                    SeckillSkuRedisTo redisTO = new SeckillSkuRedisTo();
                    //1、 SKU 的基本数据
                    R skuInfo = productFeignService.getSkuInfo(seckillSkuVO.getSkuId());
                    if (skuInfo.getCode() == 0) {
                        SkuInfoVo info = (SkuInfoVo) skuInfo.getData("skuInfo", new TypeReference<SkuInfoVo>() {});
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
