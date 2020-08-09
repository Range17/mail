package com.range.mail.order.web;

import com.alipay.api.AlipayApiException;
import com.range.mail.order.config.AlipayTemplate;
import com.range.mail.order.service.OrderService;
import com.range.mail.order.vo.PayVo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

@Controller
public class PayWebController {

    @Resource
    private AlipayTemplate alipayTemplate;

    @Resource
    private OrderService orderService;

    /**
     * 支付订单
     * 支付页面让浏览器展示
     * @param orderSn
     * @return
     * @throws AlipayApiException
     */
    @ResponseBody
    @GetMapping(value = "payOrder", produces = "text/html")
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {

        //获取订单的支付信息
        PayVo payVo = orderService.getOrderPay(orderSn);

        //返回支付数据
        return alipayTemplate.pay(payVo);
    }

}