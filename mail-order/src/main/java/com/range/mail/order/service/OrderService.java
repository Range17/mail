package com.range.mail.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.range.common.to.mq.SeckillOrderTo;
import com.range.common.utils.PageUtils;
import com.range.mail.order.entity.OrderEntity;
import com.range.mail.order.vo.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 17:06:30
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 确认订单信息
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    /**
     * 提交订单
     * @param submitVo
     * @return
     */
    SubmitOrderResponseVo submitOrder(OrderSubmitVo submitVo);

    /**
     * 获取订单状态
     * @param orderSn
     * @return
     */
    OrderEntity getOrderByOrderSn(String orderSn);

    /**
     * 关闭订单（超时未支付）
     * @param entity
     */
    void closeOrder(OrderEntity entity);


    /**
     * 获取订单的支付信息
     */
    PayVo getOrderPay(String orderSn);

    /**
     * 查询当前用户的所有订单数据
     * @param params
     * @return
     */
    PageUtils queryPageWithItems(Map<String, Object> params);

    /**
     * 处理支付宝的支付结果
     * @param vo
     * @return
     */
    String handlePayResult(PayAsyncVo vo);

    /**
     * 创建秒杀订单
     * @param seckillOrder
     */
    void createSeckillOrder(SeckillOrderTo seckillOrder);
}

