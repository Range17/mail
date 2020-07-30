package com.range.mail.mailauthserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@EnableRedisHttpSession//整合redis session
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class MailAuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MailAuthServerApplication.class, args);
    }

}
