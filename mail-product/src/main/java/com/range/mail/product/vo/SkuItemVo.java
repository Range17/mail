package com.range.mail.product.vo;

import com.range.mail.product.entity.SkuImagesEntity;
import com.range.mail.product.entity.SkuInfoEntity;
import com.range.mail.product.entity.SpuInfoDescEntity;
import lombok.Data;

import java.util.List;

@Data
public class SkuItemVo {

    private SkuInfoEntity info;

    private boolean hasStock = true;

    private List<SkuImagesEntity> images;

    private List<SkuItemSaleAttrVo> saleAttr;

    private SpuInfoDescEntity desp;

    private List<SpuItemAttrGroupVo> groupAttrs;

    private SeckillInfoVo seckillInfo;

}
