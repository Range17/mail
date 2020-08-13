package com.range.mail.seckill.scheduled;

import com.range.mail.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀商品定时上架功能
 */
@Slf4j
@Service
public class SeckillSkuScheduled {

    @Resource
    private SeckillService seckillService;

    @Resource
    private RedissonClient redissonClient;

    private final String UPLOAD_STOCK = "seckill:upload:lock";

    /**
     * 每天晚上三点定时上架最近三天需要秒杀的商品
     */
    @Scheduled(cron = "*/3 * * * * ?")
    public void uploadSeckillSkuLatest3Days() {
        // 重复上架无需处理
        log.info("上架秒杀的商品信息");
        // 上架商品分布式锁 锁的业务执行完成 状态更新完成 释放锁以后 其他操作不会被阻塞很长时间
        RLock lock = redissonClient.getLock(UPLOAD_STOCK);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            seckillService.uploadSeckillSkuLatest3Days();
        } finally {
            lock.unlock();;
        }
    }
}
