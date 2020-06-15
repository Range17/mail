package com.range.mail.member.feign;

import com.range.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("mail-coupon")
public interface CoupoFeignService {

    @GetMapping("/coupon/coupon/member/list")
    public R menberCoupons();
}
