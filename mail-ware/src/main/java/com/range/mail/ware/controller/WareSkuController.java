package com.range.mail.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.range.common.exception.BizCodeEnum;
import com.range.common.exception.NoStockException;
import com.range.mail.ware.vo.SkuHasStockVo;
import com.range.mail.ware.vo.WareSkuLockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.range.mail.ware.entity.WareSkuEntity;
import com.range.mail.ware.service.WareSkuService;
import com.range.common.utils.PageUtils;
import com.range.common.utils.R;



/**
 * 商品库存
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 23:59:48
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {

    @Autowired
    private WareSkuService wareSkuService;

    /**
     * 为订单锁定库存
     * @param vo
     * @return
     */
    @PostMapping("/lock/order")
    public R lockOrder(@RequestBody WareSkuLockVo vo) {
        try {
            Boolean stock = wareSkuService.orderLockStock(vo);
            return R.ok();
        } catch (NoStockException e) {
            return R.error(BizCodeEnum.NO_STOCK_EXCEPTION.getCode(), BizCodeEnum.NO_STOCK_EXCEPTION.getMsg());
        }
    }



    /**
     * 查询sku是否有库存
     */
    @PostMapping("/hasstock")
    public R<List<SkuHasStockVo>> getSkuHasStock(@RequestBody List<Long> skuIds){
        List<SkuHasStockVo> vos = wareSkuService.getSkuHasStock(skuIds);

        R<List<SkuHasStockVo>> ok = R.ok();
        ok.setData(vos);
        return ok;
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody WareSkuEntity wareSku){
		wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody WareSkuEntity wareSku){
		wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
