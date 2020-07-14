package com.range.mail.search.controller;

import com.range.common.exception.BizCodeEnum;
import com.range.common.to.es.SkuEsModel;
import com.range.common.utils.R;
import com.range.mail.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequestMapping("/search/save")
@RestController
public class ElasticSaveController {

    @Autowired
    ProductSaveService productSaveService;

    @PostMapping("/product")
    public R productStatesUp(@RequestBody List<SkuEsModel> skuEsModels){
        boolean flag = false;
        try{
            flag = productSaveService.productStatusUp(skuEsModels);
        }catch (Exception e){
            log.error("ElastocSaveController商品上架错误：{}",e);
//            return R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode, BizCodeEnum.PRODUCT_UP_EXCEPTION.getMessage);
        }
        if(flag){ return R.ok();}
        else {
            return R.error();
//            return R.error(BizCodeEnum.PRODUCT_UP_EXCEPTION.getCode, BizCodeEnum.PRODUCT_UP_EXCEPTION.getMessage)
        }
    }
}
