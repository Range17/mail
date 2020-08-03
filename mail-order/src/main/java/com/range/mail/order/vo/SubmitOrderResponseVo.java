package com.range.mail.order.vo;

import com.range.mail.order.entity.OrderEntity;
import lombok.Data;

@Data
/**
 * 下单操作返回数据
 */
public class SubmitOrderResponseVo {

    private OrderEntity orderEntity;

    private Integer code;//错误状态码
}
