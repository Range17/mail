package com.range.mail.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.range.common.utils.PageUtils;
import com.range.mail.ware.entity.PurchaseEntity;
import com.range.mail.ware.vo.MergeVo;

import java.util.Map;

/**
 * 采购信息
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 17:16:08
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageUnreceivePurchase(Map<String, Object> params);

    void mergePurchase(MergeVo mergeVo);
}

