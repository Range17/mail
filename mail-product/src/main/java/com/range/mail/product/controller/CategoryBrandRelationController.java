package com.range.mail.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.additional.query.impl.QueryChainWrapper;
import com.range.mail.product.entity.BrandEntity;
import com.range.mail.product.vo.BrandVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.range.mail.product.entity.CategoryBrandRelationEntity;
import com.range.mail.product.service.CategoryBrandRelationService;
import com.range.common.utils.PageUtils;
import com.range.common.utils.R;



/**
 * 品牌分类关联
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 22:36:32
 */
@RestController
@RequestMapping("product/categorybrandrelation")
public class CategoryBrandRelationController {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;



    /**
     * 获取品牌关联的分类
     */
    @GetMapping("/catelog/list")
    public R cateloglist(@RequestParam("brandId") Long brandId){

        List<CategoryBrandRelationEntity> categoryBrandRelationEntityList = categoryBrandRelationService.list(
                new QueryWrapper<CategoryBrandRelationEntity>().eq("brand_id",brandId)
        );
        return R.ok().put("data", categoryBrandRelationEntityList);
    }


    /**
     * /product/categorybrandrelation/brands/list
     */

    @GetMapping("/brands/list")
    //RequestParam中的加的为该值为必须的，没有该值就不进入方法
    public R relationBrandsList(@RequestParam(value = "catId",required = true) Long catId){
        List<BrandEntity> brandEntityList=categoryBrandRelationService.getBrandsByCatId(catId);
        List<BrandVo> brandVos = brandEntityList.stream().map(brandEntity -> {
            BrandVo vo = new BrandVo();
            vo.setBrandId(brandEntity.getBrandId());
            vo.setBrandName(brandEntity.getName());
            return vo;
        }).collect(Collectors.toList());

        return R.ok().put("data",brandVos);

    }



    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = categoryBrandRelationService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		CategoryBrandRelationEntity categoryBrandRelation = categoryBrandRelationService.getById(id);

        return R.ok().put("categoryBrandRelation", categoryBrandRelation);
    }

    /**
     * 新增品牌与分类关联管理
     */
    @RequestMapping("/save")
    public R save(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
		categoryBrandRelationService.saveDetail(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody CategoryBrandRelationEntity categoryBrandRelation){
		categoryBrandRelationService.updateById(categoryBrandRelation);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		categoryBrandRelationService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
