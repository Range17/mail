package com.range.mail.product.dao;

import com.range.mail.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-11 23:18:56
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
