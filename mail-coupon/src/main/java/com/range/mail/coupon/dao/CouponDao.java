package com.range.mail.coupon.dao;

import com.range.mail.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 17:09:40
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
