package com.range.mail.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.range.common.utils.PageUtils;
import com.range.mail.product.entity.SpuInfoDescEntity;
import com.range.mail.product.entity.SpuInfoEntity;
import com.range.mail.product.vo.SpuSaveVo;

import java.util.Map;

/**
 * spu信息
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-11 23:18:55
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);


    void saveSpuInfo( SpuSaveVo vo );

    void saveBaseSpuInfo(SpuInfoEntity infoEntity);

    PageUtils queryPageByCondition(Map<String, Object> params);

    /**
     * 商品上架
     * @param spuId
     */
    void up(Long spuId);
}

