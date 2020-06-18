package com.range.mail.product.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.range.common.utils.PageUtils;
import com.range.common.utils.Query;

import com.range.mail.product.dao.CategoryDao;
import com.range.mail.product.entity.CategoryEntity;
import com.range.mail.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    CategoryDao categoryDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查询出所有分类
        List<CategoryEntity> categoryEntityList = categoryDao.selectList(null);

        //获取所有分类下的子分类
        List<CategoryEntity> classifyCategoryEntityList = categoryEntityList.stream().filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .map((categoryEntity) -> {
                    categoryEntity.setChildCategoryEntity(getChildrenCategoryEntity(categoryEntity, categoryEntityList));
                    return categoryEntity;
                }).sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
                }).collect(Collectors.toList());

        return classifyCategoryEntityList;
    }

    /**
     * 使用递归的方法获取子菜单
     *
     * @param categoryEntity：需要获取子菜单的categoryEntity
     * @param categoryEntityList：全部分类数据
     * @return
     */
    public List<CategoryEntity> getChildrenCategoryEntity(CategoryEntity categoryEntity, List<CategoryEntity> categoryEntityList) {

        List<CategoryEntity> childCategoryEntityList = categoryEntityList.stream().filter(categoryEntity1 -> categoryEntity.getCatId().equals(categoryEntity1.getParentCid()))
                .map(categoryEntity1 -> {
                    categoryEntity1.setChildCategoryEntity(getChildrenCategoryEntity(categoryEntity1, categoryEntityList));
                    return categoryEntity1;
                }).sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
                }).collect(Collectors.toList());

        return childCategoryEntityList;
    }

}