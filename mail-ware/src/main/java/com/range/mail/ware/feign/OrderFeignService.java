package com.range.mail.ware.feign;

import com.range.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("mall-order")
public interface OrderFeignService {

    /**
     * 获取订单状态
     * @param orderSn
     * @return
     */
    @GetMapping("/order/order/status/{orderSn}")
    R getOrderStatus(@PathVariable("orderSn") String orderSn);
}
