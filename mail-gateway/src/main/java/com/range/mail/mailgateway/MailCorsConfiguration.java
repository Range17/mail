package com.range.mail.mailgateway;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
/**
 * 过滤，允许所有的请求跨域
 */
public class MailCorsConfiguration {

    @Bean
    public CorsWebFilter corsWebFilter(){
        UrlBasedCorsConfigurationSource source=new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //允许所有请求头
        corsConfiguration.addAllowedHeader("*");
        //允许所有方法
        corsConfiguration.addAllowedMethod("*");
        //允许所有来源
        corsConfiguration.addAllowedOrigin("*");
        //是否携带cookie进行过滤
        corsConfiguration.setAllowCredentials(true);

        source.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(source);
    }
}