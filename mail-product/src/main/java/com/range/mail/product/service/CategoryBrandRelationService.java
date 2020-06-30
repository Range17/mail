package com.range.mail.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.range.common.utils.PageUtils;
import com.range.mail.product.entity.BrandEntity;
import com.range.mail.product.entity.CategoryBrandRelationEntity;

import java.util.List;
import java.util.Map;

/**
 * 品牌分类关联
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-11 23:18:56
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveDetail(CategoryBrandRelationEntity categoryBrandRelation);

    void updateBrand(Long brandId, String name);

    void updateCategory(Long catId, String name);

    List<BrandEntity> getBrandsByCatId(Long catId);
}

