package com.range.mail.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.range.common.to.mq.OrderTo;
import com.range.common.to.mq.StockLockedTo;
import com.range.common.utils.PageUtils;
import com.range.mail.ware.entity.WareSkuEntity;
import com.range.mail.ware.vo.SkuHasStockVo;
import com.range.mail.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 17:16:07
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    /**
     * 获取库存
     * @param skuIds
     * @return
     */
    List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds);

    /**
     * 锁定库存
     * @param vo
     * @return
     */
    Boolean orderLockStock(WareSkuLockVo vo);

    /**
     * 解锁订单库存
     * @param to
     */
    void unlockStock(StockLockedTo to);

    /**
     * 防止订单服务卡顿，先解锁
     * @param orderTO
     */
    void unlockStock(OrderTo orderTO);
}

