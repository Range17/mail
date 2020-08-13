package com.range.mail.seckill.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyRedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        // 默认连接地址 127.0.0.1:6379
        // RedissonClient redisson = Redisson.create();

        //创建配置
        Config config = new Config();
        config.useSingleServer().setAddress("redis://112.124.19.119:6379");

        //创建redisson实例
        return Redisson.create(config);
    }
}
