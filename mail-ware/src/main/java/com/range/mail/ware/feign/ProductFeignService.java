package com.range.mail.ware.feign;

import com.range.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("mail-product")
public interface ProductFeignService {



    /**
     * 查询sku
     * @param skuId
     * @return
     */
    @GetMapping("/product/skuinf/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);

}
