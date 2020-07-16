package com.range.mail.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.range.common.utils.PageUtils;
import com.range.mail.product.entity.CategoryEntity;
import com.range.mail.product.vo.Catelog2Vo;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-11 23:18:56
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    void removeMenuByIds(List<Long> asList);

    Long[] findCateLogPath(Long catelogId);

    void updateCascade(CategoryEntity category);

    List<CategoryEntity> getLevel1Categories();

    Map<String, List<Catelog2Vo>> getCatalogJson() throws InterruptedException;
}

