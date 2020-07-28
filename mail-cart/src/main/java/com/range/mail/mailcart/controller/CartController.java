package com.range.mail.mailcart.controller;

import com.range.common.constant.AuthServerConstant;
import com.range.mail.mailcart.interceptor.CartInterceptor;
import com.range.mail.mailcart.serivce.CartSerivce;
import com.range.mail.mailcart.to.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.concurrent.ExecutionException;

@RestController
public class CartController {

    @Autowired
    CartSerivce cartSerivce;

    /**
     * 如果第一次使用jd，浏览器会给一个临时身份放在cookie，user-key：标识用户身份，一个月后过期
     * 浏览器访问的时候会带上cookie
     *
     * 登录：有session
     * 没登录：按照cookie中的user-key
     *
     * 如果没有临时用户，那么创建一个临时用户
     *
     * 使用拦截器实现上述功能
     * @param httpSession
     * @return
     */
    @GetMapping
    public String cartListPage(HttpSession httpSession) {

        //1、快速得到用户信息  id,user-key
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        return "cartList";
    }

    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num,
                            RedirectAttributes attributes) throws ExecutionException, InterruptedException {
        cartSerivce.addToCart(skuId, num);
        attributes.addAttribute("skuId", skuId);
        return "redirect:http://cart.catmall.com/addToCartSuccess.html";
    }
}
