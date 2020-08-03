package com.range.mail.order.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class MallFeignConfig {
    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor() {
        return template -> {

            //RequestContextHolder.getRequestAttributes();获取一开始请求的数据
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            //同步请求头数据 cookie
            if (!ObjectUtils.isEmpty(requestAttributes)) {
                HttpServletRequest request = requestAttributes.getRequest();// 老请求
                if (!ObjectUtils.isEmpty(request)) {
                    String cookie = request.getHeader("Cookie");
                    //给新请求带上cookie
                    template.header("Cookie", cookie);
                }
            }
        };
    }
}
