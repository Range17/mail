package com.range.mail.product.dao;

import com.range.mail.product.entity.AttrEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品属性
 * 
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-11 23:18:56
 */
@Mapper
public interface AttrDao extends BaseMapper<AttrEntity> {

    List<Long> selectSearchAttrIds(@Param("attrIds")List<Long> attrIds);
}
