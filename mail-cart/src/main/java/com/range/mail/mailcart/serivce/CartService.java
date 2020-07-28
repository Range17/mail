package com.range.mail.mailcart.serivce;

import com.range.mail.mailcart.vo.CartItem;

import java.util.concurrent.ExecutionException;

public interface CartService {

    /**
     * 添加到购物车
     * @param skuId
     * @param num
     * @return
     */
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    CartItem getCartItem(Long skuId);
}
