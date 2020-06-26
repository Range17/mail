package com.range.mail.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.range.common.utils.PageUtils;
import com.range.mail.product.entity.AttrEntity;
import com.range.mail.product.vo.AttrGroupRelationVo;
import com.range.mail.product.vo.AttrRespVo;
import com.range.mail.product.vo.AttrVo;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-11 23:18:56
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveAttr(AttrVo attr);

    PageUtils queryBaseAttrPage(Map<String, Object> params, Long cateLogId,String type);

    AttrRespVo getAttrInfo(Long attrId);

    void updateAttr(AttrVo attr);

    List<AttrEntity> getRelationAttr(Long attrgroupId);

    void deleteRelation(AttrGroupRelationVo[] vos);

    PageUtils getNoRelationAttr(Long attrgroupId, Map<String, Object> params);
}

