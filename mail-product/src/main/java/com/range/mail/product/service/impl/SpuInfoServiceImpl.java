package com.range.mail.product.service.impl;

import com.range.common.constant.ProductConstant;
import com.range.common.exception.RRException;
import com.range.common.to.SkuReductionTo;
import com.range.common.to.SpuBoundTo;
import com.range.common.to.es.SkuEsModel;
import com.range.common.utils.R;
import com.range.mail.product.entity.*;
import com.range.mail.product.feign.CouponFeignService;
import com.range.mail.product.feign.SearchFeignService;
import com.range.mail.product.feign.WareFeignService;
import com.range.mail.product.service.*;
import com.range.mail.product.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.range.common.utils.PageUtils;
import com.range.common.utils.Query;

import com.range.mail.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService spuInfoDescService;

    @Autowired
    SpuImagesService spuImagesService;

    @Autowired
    AttrService attrService;

    @Autowired
    ProductAttrValueService attrValueService;

    @Autowired
    SkuInfoService skuInfoService;

    @Autowired
    SkuImagesService skuImagesService;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Autowired
    CouponFeignService couponFeignService;

    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    WareFeignService wareFeignService;

    @Autowired
    SearchFeignService searchFeignService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageUtils(page);
    }


    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {

        //1、保存基本信息 pms_spu_info
        SpuInfoEntity infoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo,infoEntity);
        infoEntity.setCreateTime(new Date());
        infoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(infoEntity);


        //2、保存Spu的描述图片 pms_spu_info_Desc
        List<String> descript  = vo.getDecript();
        SpuInfoDescEntity spuInfoDescEntity = new SpuInfoDescEntity();
        spuInfoDescEntity.setSpuId(infoEntity.getId());
        spuInfoDescEntity.setDecript(String.join(",",descript));
        spuInfoDescService.saveSpuInfoDesc(spuInfoDescEntity);


        //3、保存spu的图片集 pms_spu_images
        List<String> images = vo.getImages();
        spuImagesService.saveImages(infoEntity.getId(),images);


        //4、保存spu的规格参数 pms_product_attr_value
        List<BaseAttrs> baseAttrs = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = baseAttrs.stream().map(attr->{
            ProductAttrValueEntity valueEntity = new ProductAttrValueEntity();
            valueEntity.setAttrId(attr.getAttrId());
            AttrEntity id = attrService.getById(attr.getAttrId());
            valueEntity.setAttrName(id.getAttrName());
            valueEntity.setAttrValue(attr.getAttrValues());
            valueEntity.setQuickShow(attr.getShowDesc());
            valueEntity.setSpuId(infoEntity.getId());
            return valueEntity;
        }).collect(Collectors.toList());

        attrValueService.saveProductAttr(collect);


        //5、保存spu的积分信息：sms_spu_bounds
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds,spuBoundTo);
        spuBoundTo.setSpuId(infoEntity.getId());
        try{
            R r = couponFeignService.saveSpuBounds(spuBoundTo);
            if(r.getCode() != 0){
                log.error("远程保存积分信息调用失败");
            }
        }catch (Exception e){
            log.error("调用服务超时");
            e.printStackTrace();
        }



        //5、保存当前spu对应的所有sku信息
        List<Skus> skuses = vo.getSkus();
        if(skuses != null && skuses.size()>0){
            skuses.forEach(item->{

                String defaultImg = "";
                for (Images image:item.getImages()) {
                    if (image.getDefaultImg() == 1){
                        defaultImg = image.getImgUrl();
                    }
                }

                SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                BeanUtils.copyProperties(item,skuInfoEntity);
                skuInfoEntity.setBrandId(infoEntity.getBrandId());
                skuInfoEntity.setCatalogId(infoEntity.getCatalogId());
                skuInfoEntity.setSaleCount(0L);
                skuInfoEntity.setSpuId(infoEntity.getId());
                skuInfoEntity.setSkuDefaultImg(defaultImg);
                //1、sku的基本信息：pms_sku_info
                skuInfoService.saveSkuInfo(skuInfoEntity);

                Long skuId = skuInfoEntity.getSkuId();

                List<SkuImagesEntity> imagesEntities = item.getImages().stream().map(img ->{
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setSkuId(skuId);
                    skuImagesEntity.setImgUrl(img.getImgUrl());
                    skuImagesEntity.setDefaultImg(img.getDefaultImg());
                    return skuImagesEntity;
                }).filter(entity->{
                    //返回true就是需要，返回false就是剔除
                    return !StringUtils.isEmpty(entity.getImgUrl());
                }).collect(Collectors.toList());
                //2、sku的图片信息：pms_sku_images
                skuImagesService.saveBatch(imagesEntities);


                //3、sku的销售属性信息:pms_sku_sale_attr_value
                List<Attr> attrs = item.getAttr();
                List<SkuSaleAttrValueEntity> skuSaleAttrValueEntities = attrs.stream().map(attr -> {
                    SkuSaleAttrValueEntity skuSaleAttrValueEntity = new SkuSaleAttrValueEntity();
                    BeanUtils.copyProperties(attr,skuSaleAttrValueEntity);
                    return skuSaleAttrValueEntity;
                }).collect(Collectors.toList());
                skuSaleAttrValueService.saveBatch(skuSaleAttrValueEntities);


                //4、sku的优惠、满减等信息：gulimail_sms->sms_sku_ladder\sms_sku_full_reduction\sms_member_price
                SkuReductionTo skuReductionTo = new SkuReductionTo();
                BeanUtils.copyProperties(item,skuReductionTo);
                skuReductionTo.setSkuId(skuId);
                if (skuReductionTo.getFullCount() > 0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0")) == 1) {
                    try {
                        R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                        if (r1.getCode() != 0) {
                            log.error("远程保存sku优惠信息调用失败");
                        }
                    }catch (Exception e){
                        throw new RRException("调用远程服务超时");
                    }
                }
            });
        }
    }

    @Override
    public void saveBaseSpuInfo(SpuInfoEntity infoEntity) {
        this.baseMapper.insert(infoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.and((w)->{
                w.eq("id",key).or().like("spu_name",key);
            });
        }

        String status = (String) params.get("status");
        if(!StringUtils.isEmpty(status)){
            wrapper.eq("publish_status",status);
        }

        String brandId = (String) params.get("brandId");
        if(!StringUtils.isEmpty(brandId)&&(!"0".equalsIgnoreCase(brandId))){
            wrapper.eq("brand_id",brandId);

        }

        String catelogId = (String)params.get("catelogId");
        if(!StringUtils.isEmpty(catelogId)&&(!"0".equalsIgnoreCase(catelogId))){
            wrapper.eq("catelog_id",catelogId);
        }

        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),wrapper
        );

        return new PageUtils(page);    }

    @Override
    public void up(Long spuId) {

        //查出当前spuId对应的所有sku信息、品牌的名字
        List<SkuInfoEntity> skus = skuInfoService.getSkusBySpuId(spuId);

        List<Long> skuIdList = skus.stream().map(SkuInfoEntity::getSkuId).collect(Collectors.toList());

        //todo 4、查询sku的所有可以被用来检索规格属性
        List<ProductAttrValueEntity> baseAttrs = attrValueService.baseAttrlistforspu(spuId);
        List<Long> attrIds = baseAttrs.stream().map(attr ->{
            return attr.getAttrId();
        }).collect(Collectors.toList());

        List<Long> searchAttrIds = attrService.selectSearchAttrIds(attrIds);
        Set<Long> idSet = new HashSet<>(searchAttrIds);

        List<SkuEsModel.Attrs> attrsList = baseAttrs.stream().filter(item->{
            return idSet.contains(item.getAttrId());
        }).map(item->{
            SkuEsModel.Attrs attrs1 = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item,attrs1);
            return attrs1;
        }).collect(Collectors.toList());



        //todo 1、发送远程调用，库存系统查询是否有库存
        Map<Long,Boolean> stockMap = null;
        try{
            R<List<SkuHasStockVo>> skusHasStock = wareFeignService.getSkuHasStock(skuIdList);
            stockMap = skusHasStock.getData().stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId,item->item.isHasStock()));
        }catch (Exception e){
            log.error("库存服务查询异常:原因{}",e);
        }


        //封装每个sku信息
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> upProducts = skus.stream().map(sku -> {
            SkuEsModel esModel = new SkuEsModel();
            BeanUtils.copyProperties(sku,esModel);
            esModel.setSkuPrice(sku.getPrice());
            esModel.setSkuImg(sku.getSkuDefaultImg());

            esModel.setHasStock(finalStockMap.get(sku.getSkuId()));

            //todo 2、热度评分。0
            esModel.setHotScore(0L);

            //todo 3、查询品牌和分类的名字信息
            BrandEntity brand = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(brand.getName());
            esModel.setBrandImg(brand.getLogo());

            CategoryEntity categoryEntity = categoryService.getById(esModel.getBrandId());
            esModel.setCatalogName(categoryEntity.getName());

            //设置检索属性
            esModel.setAttrs(attrsList);

            return esModel;
        }).collect(Collectors.toList());

        //todo 5、将数据发送到es进行保存

        R r = searchFeignService.productStatesUp(upProducts);
        if(r.getCode() == 0){
            //远程调用成功
            //修改当前spu的状态
            baseMapper.updateSpuStatus(spuId, ProductConstant.StateEnum.SPU_UP.getCode());
        }else{
            //远程调用失败
            //todo 重复调用问题？
            /**
             * 1、构造请求数据，将对象转为json
             * 2、发送请求进行执行（执行成功会解码响应数据）
             * 3、执行请求会有重试机制
             */
        }

    }


}