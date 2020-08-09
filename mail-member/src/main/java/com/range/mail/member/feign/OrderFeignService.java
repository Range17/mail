package com.range.mail.member.feign;

import com.range.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@FeignClient("mail-order")
public interface OrderFeignService {

    @RequestMapping("/order/order/listWithItems")
    R listWithItems(@RequestBody Map<String, Object> params);

}

