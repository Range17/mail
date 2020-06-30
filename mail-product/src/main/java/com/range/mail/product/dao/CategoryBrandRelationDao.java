package com.range.mail.product.dao;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.range.mail.product.entity.BrandEntity;
import com.range.mail.product.entity.CategoryBrandRelationEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 品牌分类关联
 * 
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-11 23:18:56
 */
@Mapper
public interface CategoryBrandRelationDao extends BaseMapper<CategoryBrandRelationEntity> {

    void updateCategory(@Param("catId") Long catId, @Param("name") String name);

}
