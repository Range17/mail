package com.range.mail.order.dao;

import com.range.mail.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 17:06:30
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
