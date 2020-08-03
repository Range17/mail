package com.range.mail.order.web;

import com.range.common.exception.NoStockException;
import com.range.mail.order.service.OrderService;
import com.range.mail.order.vo.OrderConfirmVo;
import com.range.mail.order.vo.OrderSubmitVo;
import com.range.mail.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.ExecutionException;

public class OrderWebController {

    @Autowired
    OrderService orderService;

    /**
     * 确认订单，通过拦截器获取用户，再根据用户去获取购物车以及相应的其他信息
     * @param model
     * @param request
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @GetMapping("/toTrade")
    public String toTrade(Model model, HttpServletRequest request) throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = orderService.confirmOrder();
        model.addAttribute("orderConfirmData", orderConfirmVo);
        return "confirm";
    }

    /**
     * 下单功能
     * @param submitVo
     * @param model
     * @param redirectAttributes
     * @return
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo submitVo, Model model, RedirectAttributes redirectAttributes) {
        // 下单 去创建订单 验证令牌 核算价格 锁定库存
        try {
            SubmitOrderResponseVo responseVo = orderService.submitOrder(submitVo);
            if (responseVo.getCode() == 0) {
                // 下单成功到选择支付方式页面
                model.addAttribute("submitOrderResponse", responseVo);
                return "pay";
            } else {
                // 订单失败返回到订单确认页面
                String msg = "下订单失败: ";
                switch (responseVo.getCode()) {
                    case 1 : msg += "订单信息过期, 请刷新后再次提交."; break;
                    case 2 : msg += "订单中的商品价格发生变化, 请刷新后再次提交."; break;
                    case 3 : msg += "库存锁定失败, 商品库存不足."; break;
                }
                redirectAttributes.addFlashAttribute("msg", msg);
                return "redirect:http://order.catmall.com/toTrade";
            }
        } catch (Exception e) {
            if (e instanceof NoStockException) {
                String message = e.getMessage();
                redirectAttributes.addFlashAttribute("msg", message);
            }
            return "redirect:http://order.catmall.com/toTrade";
        }
    }
}
