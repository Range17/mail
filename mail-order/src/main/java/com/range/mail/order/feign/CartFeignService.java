package com.range.mail.order.feign;

import com.range.mail.order.vo.OrderItemVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("mail-cart")
public interface CartFeignService {

    /**
     * 获取当前用户的购物车数据
     * @return
     */
    @GetMapping("/currentUserCartItems")
    List<OrderItemVo> getCurrentUserCartItems();

}