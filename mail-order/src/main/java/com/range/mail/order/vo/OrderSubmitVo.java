package com.range.mail.order.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 封装订单提交数据
 */
@Data
public class OrderSubmitVo {

    private Long addrId;

    private Integer payType; // 支付方式

    private String orderToken;

    private BigDecimal payPrice; // 应付价格 验价

    private String note; // 订单备注


}
