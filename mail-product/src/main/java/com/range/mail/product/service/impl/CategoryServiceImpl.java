package com.range.mail.product.service.impl;

import com.range.mail.product.entity.CategoryBrandRelationEntity;
import com.range.mail.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
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

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

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
                .map(categoryEntity -> {
                    categoryEntity.setChildren(getChildrenCategoryEntity(categoryEntity, categoryEntityList));
                    return categoryEntity;
                }).sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
                }).collect(Collectors.toList());

        return classifyCategoryEntityList;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {

        //TODO 1.检查当前删除的菜单是否被别的地方引用
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCateLogPath(Long catelogId) {
        List<Long> path = new ArrayList<>();
        List<Long> parentPath = findParentPath(catelogId, path);
        //将集合顺序反转了
        Collections.reverse(parentPath);
        return parentPath.toArray(new Long[path.size()]);
    }

    /**
     * 级联更新所有关联的数据
     * @param category
     */
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());


    }

    private List<Long> findParentPath(Long catelogId, List<Long> path) {
        // 收集当前节点
        path.add(catelogId);
        CategoryEntity categoryEntity = this.getById(catelogId);
        if (categoryEntity.getParentCid() != 0) {
            findParentPath(categoryEntity.getParentCid(), path);
        }
        return path;
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
                    categoryEntity1.setChildren(getChildrenCategoryEntity(categoryEntity1, categoryEntityList));
                    return categoryEntity1;
                }).sorted((menu1, menu2) -> {
                    return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
                }).collect(Collectors.toList());

        return childCategoryEntityList;
    }

}