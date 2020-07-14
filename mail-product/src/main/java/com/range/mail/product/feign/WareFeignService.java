package com.range.mail.product.feign;

import com.range.common.utils.R;
import com.range.mail.product.vo.SkuHasStockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("mail-ware")
public interface WareFeignService {

    @PostMapping("/ware/waresku/hasstock")
    R<List<SkuHasStockVo>> getSkuHasStock(@RequestBody List<Long> skuIds);
}
