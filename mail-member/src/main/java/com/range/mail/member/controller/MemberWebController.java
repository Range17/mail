package com.range.mail.member.controller;

import com.range.common.utils.R;
import com.range.mail.member.feign.OrderFeignService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MemberWebController {

    @Resource
    private OrderFeignService orderFeignService;

    /**
     * 使用 cookie 作为标识获取数据，需要加入 MallFeignConfig 来保存请求头
     * @return
     */
    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum, Model model) {
        // 查出当前登录的用户的所有订单列表数据
        Map<String, Object> page = new HashMap<>();
        page.put("page", String.valueOf(pageNum));
        R r = orderFeignService.listWithItems(page);
        model.addAttribute("orders", r);
        return "orderList";
    }

}