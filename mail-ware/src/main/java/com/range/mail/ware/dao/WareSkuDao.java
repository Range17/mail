package com.range.mail.ware.dao;

import com.range.mail.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 商品库存
 * 
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 17:16:07
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {
    void addStock(@Param("skuId")Long skuId, @Param("wareId")Long wareId, @Param("skuNum")Integer skuNum);
}
