package com.range.mail.product.feign;

import com.range.common.to.es.SkuEsModel;
import com.range.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("mail-search")
public interface SearchFeignService {

    @PostMapping("/search/save/product")
    R productStatesUp(@RequestBody List<SkuEsModel> skuEsModels);
}
