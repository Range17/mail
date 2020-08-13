package com.range.mail.seckill.service;

import com.range.mail.seckill.to.SeckillSkuRedisTo;

import java.util.List;

public interface SeckillService {

    /**
     * 最近三天需要参与秒活动的商品
     */
    void uploadSeckillSkuLatest3Days();

    /**
     * 返回当前时间可以参与的秒杀活动
     * @return
     */
    List<SeckillSkuRedisTo> getCurrentSeckillSkus();

    /**
     * 查询商品是否参与秒杀
     * @param skuId
     * @return
     */
    SeckillSkuRedisTo getSkuSeckillInfo(Long skuId);

    /**
     * 秒杀方法
     * @param killId
     * @param key
     * @param num
     * @return
     */
    String kill(String killId, String key, Integer num);

}
