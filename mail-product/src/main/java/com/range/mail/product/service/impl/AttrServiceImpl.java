package com.range.mail.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.range.common.constant.ProductConstant;
import com.range.mail.product.dao.AttrAttrgroupRelationDao;
import com.range.mail.product.dao.AttrGroupDao;
import com.range.mail.product.dao.CategoryDao;
import com.range.mail.product.entity.AttrAttrgroupRelationEntity;
import com.range.mail.product.entity.AttrGroupEntity;
import com.range.mail.product.entity.CategoryEntity;
import com.range.mail.product.service.CategoryService;
import com.range.mail.product.vo.AttrGroupRelationVo;
import com.range.mail.product.vo.AttrRespVo;
import com.range.mail.product.vo.AttrVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.range.common.utils.PageUtils;
import com.range.common.utils.Query;

import com.range.mail.product.dao.AttrDao;
import com.range.mail.product.entity.AttrEntity;
import com.range.mail.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Autowired
    private AttrGroupDao attrGroupDao;

    @Autowired
    private CategoryDao categoryDao;

    @Autowired
    private CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        //1、保存基本数据
        this.save(attrEntity);

        //2、保存关联关系
        if(attr.getAttrType() == ProductConstant.AttrEnum.ATTR_ENUM_BASE.getCode() && attr.getAttrGroupId() != null){
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();
            attrAttrgroupRelationEntity.setAttrGroupId(attr.getAttrGroupId());
            attrAttrgroupRelationEntity.setAttrId(attrEntity.getAttrId());
            attrAttrgroupRelationDao.insert(attrAttrgroupRelationEntity);
        }

    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long cateLogId,String type) {

        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>().eq("attr_type","base".equalsIgnoreCase(type)?ProductConstant.AttrEnum.ATTR_ENUM_BASE.getCode():ProductConstant.AttrEnum.ATTR_ENUM_SALE.getCode());

        if (cateLogId != 0) {
            queryWrapper.eq("catelog_id", cateLogId);
        }
        String key = (String) params.get("key");

        //当存在key、需要封装查询条件
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((wrapper) -> {
                wrapper.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo> respVoList = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);

            //1、设置分组的名字
            if("base".equalsIgnoreCase(type)){
                AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
                if (attrAttrgroupRelationEntity != null && attrAttrgroupRelationEntity.getAttrGroupId()!=null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrAttrgroupRelationEntity.getAttrGroupId());
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }


            //2、设置分类的名字
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVo.setCatelogName(categoryEntity.getName());
            }

            return attrRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(respVoList);
        return pageUtils;
    }

    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo attrRespVo = new AttrRespVo();
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, attrRespVo);


        if(attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_ENUM_BASE.getCode()){
            //1、设置分组信息
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if (attrAttrgroupRelationEntity != null) {
                attrRespVo.setAttrGroupId(attrAttrgroupRelationEntity.getAttrGroupId());
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrAttrgroupRelationEntity.getAttrGroupId());
                if (attrGroupEntity != null) {
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }


        //2、设置分类信息
        Long catelogId = attrEntity.getCatelogId();
        Long[] catelogPath = categoryService.findCateLogPath(catelogId);
        attrRespVo.setCatelogpath(catelogPath);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if (categoryEntity != null) {
            attrRespVo.setCatelogName(categoryEntity.getName());
        }


        return attrRespVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.updateById(attrEntity);

        if(attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_ENUM_BASE.getCode()){
            //1、修改分组关联
            AttrAttrgroupRelationEntity attrAttrgroupRelationEntity = new AttrAttrgroupRelationEntity();

            attrAttrgroupRelationEntity.setAttrGroupId(attr.getAttrGroupId());
            attrAttrgroupRelationEntity.setAttrId(attr.getAttrId());

            Integer count = attrAttrgroupRelationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            if (count > 0) {
                attrAttrgroupRelationDao.update(attrAttrgroupRelationEntity, new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            } else {
                attrAttrgroupRelationDao.insert(attrAttrgroupRelationEntity);
            }
        }

    }

    /**
     * 根据分组id查找关联的所有基本属性
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {

        List<AttrAttrgroupRelationEntity> entities = attrAttrgroupRelationDao.selectList(new QueryWrapper<>());
        List<Long> attrIds = entities.stream().map((attrAttrgroupRelationEntity)->{
            return attrAttrgroupRelationEntity.getAttrId();
        }).collect(Collectors.toList());

        if(attrIds == null || attrIds.size() == 0){
            return null;
        }
        Collection<AttrEntity> attrEntities = this.listByIds(attrIds);
        return (List<AttrEntity>) attrEntities;
    }

    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) {

        List<AttrAttrgroupRelationEntity> entities= Arrays.asList(vos).stream().map((item)->{
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item,relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());
        attrAttrgroupRelationDao.deleteBatchRelation(entities);
    }

    /**
     * 获取当前分组没有关联的所有属性
     * @param attrgroupId
     * @param params
     * @return
     */
    @Override
    public PageUtils getNoRelationAttr(Long attrgroupId, Map<String, Object> params) {
        //1、当前分组只能关联自己所属的分类里面的所有属性
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();

        //2、当前分组只能关联别的分组没有引用的属性
        //2.1、当前分类下的其他分组
        List<AttrGroupEntity> group = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id",catelogId));
        List<Long> collect = group.stream().map(item->{
            return item.getAttrGroupId();
        }).collect(Collectors.toList());
        //2.2、这些分组关联的属性
        List<AttrAttrgroupRelationEntity> groupId = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id",collect));
        List<Long> attrIds = groupId.stream().map(item->{
            return item.getAttrId();
        }).collect(Collectors.toList());

        //2.3、从当前分类的所有属性中移除这些属性
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>().eq("catelog_id",catelogId).eq("attr_type",ProductConstant.AttrEnum.ATTR_ENUM_BASE.getCode());

        if(attrIds!=null&& attrIds.size()>0){
            wrapper.notIn("attr_id",attrIds);
        }

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and((w)->{
                w.eq("attr_id",key).or().like("attr_name",key);
            });
        }
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params),wrapper);
        PageUtils pageUtils = new PageUtils(page);
        return pageUtils;
    }

}