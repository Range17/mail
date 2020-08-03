package com.range.mail.order.to;

import com.range.mail.order.entity.OrderEntity;
import com.range.mail.order.entity.OrderItemEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
/**
 * 创建订单
 */
public class OrderCreateTo {

    private OrderEntity orderEntity;

    private List<OrderItemEntity> orderItems;

    private BigDecimal payPrice;

    private BigDecimal fare;

}
