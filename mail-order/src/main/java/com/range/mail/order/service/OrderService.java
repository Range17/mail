package com.range.mail.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.range.common.utils.PageUtils;
import com.range.mail.order.entity.OrderEntity;

import java.util.Map;

/**
 * 订单
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 17:06:30
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

