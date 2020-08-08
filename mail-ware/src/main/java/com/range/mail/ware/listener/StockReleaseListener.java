package com.range.mail.ware.listener;

import com.rabbitmq.client.Channel;
import com.range.common.to.mq.OrderTo;
import com.range.common.to.mq.StockLockedTo;
import com.range.mail.ware.service.WareSkuService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;


/**
 * 解锁消息监听器
 */
@Service
@RabbitListener(queues = {"stock.release.stock.queue"})
public class StockReleaseListener {

    @Resource
    private WareSkuService wareSkuService;

    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        System.out.println("收到解锁库存的消息...");
        try {
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo orderTO, Message message, Channel channel) throws IOException {
        System.out.println("订单关闭，准备解锁库存...");
        try {
            wareSkuService.unlockStock(orderTO);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

}
