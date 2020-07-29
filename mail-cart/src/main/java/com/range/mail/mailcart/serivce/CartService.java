package com.range.mail.mailcart.serivce;

import com.range.mail.mailcart.vo.Cart;
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

    /**
     * 获取购物车内容
     * @param skuId
     * @return
     */
    CartItem getCartItem(Long skuId);

    /**
     *
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    Cart getCart() throws ExecutionException, InterruptedException;

    /**
     * 清除购物车
     * @param cartKey
     */
    void clearCart(String cartKey);

    /**
     * 勾选购物项
     * @param skuId
     * @param checked
     */
    void checkItem(Long skuId, Integer checked);

    /**
     * 删除购物车
     * @param skuId
     */
    void deleteItem(Long skuId);

    /**
     * 计算商品
     * @param skuId
     * @param num
     */
    void countItem(Long skuId, Integer num);
}
