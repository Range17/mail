package com.range.mail.seckill.service;

import com.range.mail.seckill.to.SeckillSkuRedisTo;

import java.util.List;

public interface SeckillService {

    /**
     * 最近三天需要参与秒活动的商品
     */
    void uploadSeckillSkuLatest3Days();

    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    SeckillSkuRedisTo getSkuSeckillInfo(Long skuId);

    String kill(String killId, String key, Integer num);

}
