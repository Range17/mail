package com.range.mail.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
/**
 * 计算会员运费
 */
public class FareVo {

    private MemberAddressVo memberAddressVo;

    private BigDecimal fare;
}
