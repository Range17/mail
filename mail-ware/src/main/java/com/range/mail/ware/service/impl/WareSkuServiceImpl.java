package com.range.mail.ware.service.impl;

import com.range.common.exception.NoStockException;
import com.range.common.exception.RRException;
import com.range.common.utils.R;
import com.range.mail.ware.entity.WareOrderTaskDetailEntity;
import com.range.mail.ware.entity.WareOrderTaskEntity;
import com.range.mail.ware.feign.ProductFeignService;
import com.range.mail.ware.service.WareOrderTaskDetailService;
import com.range.mail.ware.vo.OrderItemVo;
import com.range.mail.ware.vo.SkuHasStockVo;
import com.range.mail.ware.vo.WareSkuLockVo;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.range.common.utils.PageUtils;
import com.range.common.utils.Query;

import com.range.mail.ware.dao.WareSkuDao;
import com.range.mail.ware.entity.WareSkuEntity;
import com.range.mail.ware.service.WareSkuService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    WareSkuDao wareSkuDao;

    @Autowired
    ProductFeignService productFeignService;

    @Autowired
    WareOrderTaskDetailService wareOrderTaskDetailService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {


        QueryWrapper<WareSkuEntity> queryWrapper = new QueryWrapper<>();

        String skuId = (String) params.get("skuId");
        if(!StringUtils.isEmpty(skuId)){
            queryWrapper.eq("sku_id",skuId);
        }


        String wareId = (String) params.get("wareId");
        if(!StringUtils.isEmpty(wareId)){
            queryWrapper.eq("ware_id",wareId);
        }


        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        //1、判断是否存在库存记录
        List<WareSkuEntity> entities = wareSkuDao.selectList(new QueryWrapper<WareSkuEntity>().eq("sku_id", skuId).eq("ware_id",wareId));
        if (entities.size() == 0||entities == null){
            WareSkuEntity skuEntity = new WareSkuEntity();
            skuEntity.setSkuId(skuId);
            skuEntity.setStock(skuNum);
            skuEntity.setWareId(wareId);
            skuEntity.setStock(0);

            //远程查询sku名称
            try{
                R info = productFeignService.info(skuId);
                Map<String,Object> map = (Map<String, Object>) info.get("data");
                if(info.getCode() == 0){
                    skuEntity.setSkuName((String) map.get("skuName"));
                }
            }catch (Exception e){
                throw new RRException("调用远程服务超时");
            }


            wareSkuDao.insert(skuEntity);
        }

        wareSkuDao.addStock(skuId, wareId, skuNum);
    }

    @Override
    public List<SkuHasStockVo> getSkuHasStock(List<Long> skuIds) {

        List<SkuHasStockVo> collect = skuIds.stream().map(skuId->{
            SkuHasStockVo skuHasStockVo = new SkuHasStockVo();

            //查询出当前的sku总库存量
            Long count = baseMapper.getSkuStock(skuId);
            skuHasStockVo.setSkuId(skuId);
            skuHasStockVo.setHasStock(count==null?false:count>0);
            return skuHasStockVo;
        }).collect(Collectors.toList());

        return collect;
    }

    //@Transactional默认是运行时异常都会回滚
    @Transactional(rollbackFor = NoStockException.class)
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        // 按照下单的收货地址 找到一个就近的仓库 锁定库存

        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        // 找到每个商品在哪些仓库还有库存
        List<OrderItemVo> locks = vo.getLocks();

        List<SkuWareHasStock> collect = locks.stream().map(o -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = o.getSkuId();
            stock.setSkuId(skuId);
            stock.setNum(o.getCount());
            //查询商品在哪里有库存
            List<Long> wareIds = wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareIds(wareIds);
            return stock;
        }).collect(Collectors.toList());


        //锁定库存
        for (SkuWareHasStock hasStock : collect) {
            Boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareIds();


            if (CollectionUtils.isEmpty(wareIds)) {
                throw new NoStockException(skuId);
            }

            //遍历仓库，减库存
            for (Long wareId : wareIds) {
                //锁定库存 成功返回 1 失败返回 0
                Long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count == 1) {
                    skuStocked = true;
                    // 告诉 MQ 库存锁定成功
//                    WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity(null, skuId, "", hasStock.getNum(), taskEntity.getId(), wareId, 1);
//                    wareOrderTaskDetailService.save(wareOrderTaskDetailEntity);
//                    StockLockedTo stockLockedTO = new StockLockedTo();
//                    StockDetailTo stockDetailTO = new StockDetailTo();
//                    BeanUtils.copyProperties(wareOrderTaskDetailEntity, stockDetailTO);
//                    stockLockedTO.setId(taskEntity.getId());
//                    stockLockedTO.setDetail(stockDetailTO);
//                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTO);
                    break;
                } else {
                    // 当前仓库锁定库存失败 重试下一个仓库
                }
            }
            if (!skuStocked) {
                //当前商品所有仓库都没有锁住
                throw new NoStockException(skuId);
            }
        }

        //锁定库存成功
        return true;
    }

    @Data
    static class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareIds;
    }


}