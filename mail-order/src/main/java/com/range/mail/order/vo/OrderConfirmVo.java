package com.range.mail.order.vo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderConfirmVo {

    @Getter
    @Setter
    /**
     * 收货地址
     */
    private List<MemberAddressVo> addresses;

    @Getter
    @Setter
    /**
     * 所有选中的购物项
     */
    private List<OrderItemVo> items;

    @Getter
    @Setter
    private String orderToken; // 防重令牌

    // 发票记录

    @Getter
    @Setter
    private Map<Long, Boolean> stocks;

    // 优惠券信息
    @Getter
    @Setter
    private Integer integration;

    public Integer getCount() {
        Integer count = 0;
        if (!CollectionUtils.isEmpty(items)) {
            for (OrderItemVo orderItemVo : items) {
                count += orderItemVo.getCount();
            }
        }
        return count;
    }

    // private BigDecimal total; // 订单总额
    public BigDecimal getTotal() {
        BigDecimal sum = new BigDecimal("0");
        if (!CollectionUtils.isEmpty(items)) {
            for (OrderItemVo itemVo : items) {
                BigDecimal multiply = itemVo.getPrice().multiply(new BigDecimal(itemVo.getCount().toString()));
                sum = sum.add(multiply);
            }
        }
        return sum;
    }

    // private BigDecimal payPrice; // 应付价格
    public BigDecimal getPayPrice() {
        return getTotal();
    }

}
