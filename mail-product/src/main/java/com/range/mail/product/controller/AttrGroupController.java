package com.range.mail.product.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.range.mail.product.entity.AttrAttrgroupRelationEntity;
import com.range.mail.product.entity.AttrEntity;
import com.range.mail.product.service.AttrAttrgroupRelationService;
import com.range.mail.product.service.AttrService;
import com.range.mail.product.service.CategoryService;
import com.range.mail.product.vo.AttrGroupRelationVo;
import com.range.mail.product.vo.AttrGroupWithAttrsVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.range.mail.product.entity.AttrGroupEntity;
import com.range.mail.product.service.AttrGroupService;
import com.range.common.utils.PageUtils;
import com.range.common.utils.R;



/**
 * 属性分组
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 22:36:32
 */
@RestController
@RequestMapping("product/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AttrService attrService;

    @Autowired
    private AttrAttrgroupRelationService relationService;


    @GetMapping("/{catelogId}/withattr")
    public R getAttrGroupWithAttrs(@PathVariable("catelogId") Long catelogId){

        List<AttrGroupWithAttrsVo> attrGroupWithAttrsVos = attrGroupService.getAttrGroupWithAttrsByCatelogId(catelogId);
        return R.ok().put("data",attrGroupWithAttrsVos);
    }

    @PostMapping("/attr/relation")
    public R addRelation(@RequestBody List<AttrGroupRelationVo> vos){
        relationService.saveBatch(vos);

        return R.ok();

    }

    @PostMapping("/attr/relation/delete")
    public R deleteRelation(AttrGroupRelationVo[] vos){
        attrService.deleteRelation(vos);
        return R.ok();
    }

    @GetMapping("/{attrgroupid}/attr/relation")
    public R attrRelation(@PathVariable("attrgroupId") Long attrgroupId){
        List<AttrEntity> attrEntities = attrService.getRelationAttr(attrgroupId);
        return R.ok().put("data",attrEntities);
    }


    @GetMapping("/{attrgroupid}/noattr/relation")
    public R attrNoRelation(@PathVariable("attrgroupId") Long attrgroupId,@RequestParam Map<String, Object> params){
        PageUtils page = attrService.getNoRelationAttr(attrgroupId,params);
        return R.ok().put("page",page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list/{cateLogId}")
    public R list(@RequestParam Map<String, Object> params,@PathVariable("cateLogId") Long catelogId){
//        PageUtils page = attrGroupService.queryPage(params);
        PageUtils page = attrGroupService.queryPage(params,catelogId);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrGroupId}")
    public R info(@PathVariable("attrGroupId") Long attrGroupId){
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);
		Long catelogId = attrGroup.getCatelogId();
        Long[] catelogIdList = categoryService.findCateLogPath(catelogId);
        attrGroup.setCatelogPath(catelogIdList);
        return R.ok().put("attrGroup", attrGroup);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return R.ok();
    }

}
