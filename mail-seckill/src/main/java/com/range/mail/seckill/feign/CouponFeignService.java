package com.range.mail.seckill.feign;

import com.range.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("mail-coupon")
public interface CouponFeignService {

    @GetMapping("/coupon/seckillsession/latest3DaySession")
    R getLatest3DaySession();

}