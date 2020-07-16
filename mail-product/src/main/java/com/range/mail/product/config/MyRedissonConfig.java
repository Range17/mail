package com.range.mail.product.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * redisson配置
 */
@Configuration
public class MyRedissonConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() throws IOException {

        Config config = new Config();
        config.useSingleServer().setAddress("redis://112.124.19.119:6379");
        RedissonClient redissonClient = Redisson.create(config);
        return redissonClient;
    }
}
