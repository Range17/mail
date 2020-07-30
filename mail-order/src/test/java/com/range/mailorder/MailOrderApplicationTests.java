package com.range.mailorder;

import com.range.mail.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.script.Bindings;
import java.util.Date;



@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
class MailOrderApplicationTests {

    @Autowired
    AmqpAdmin amqpAdmin;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    public void createExchange(){
        DirectExchange directExchange = new DirectExchange("hello-java-excahnge",true,false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange[{}]创建成功","hello-java-excahnge");
    }

    @Test
    public void createQueue(){
        Queue queue = new Queue("hello-java-queue",true,false,false);
        amqpAdmin.declareQueue(queue);
        log.info("Queue[{}]创建成功","hello-java-queue");
    }

    @Test
    public void createBinding(){
        Binding binding = new Binding("hello-java-queue",Binding.DestinationType.QUEUE,"hello-java-exchange","hello.java",null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding[{}]创建成功","hello-java-binding");
    }

    @Test
    public void sendMessageTest(){
        OrderReturnReasonEntity returnReasonEntity = new OrderReturnReasonEntity();
        returnReasonEntity.setId(1L);
        returnReasonEntity.setCreateTime(new Date());
        returnReasonEntity.setName("test");
        String msg = "hello world";
        rabbitTemplate.convertAndSend("hello-java-excahnge", "hello.java", returnReasonEntity);
        log.info("消息发送完成{}",returnReasonEntity);
    }

}
