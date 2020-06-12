package com.range.mail.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.range.common.utils.PageUtils;
import com.range.mail.ware.entity.PurchaseDetailEntity;

import java.util.Map;

/**
 * 
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 17:16:08
 */
public interface PurchaseDetailService extends IService<PurchaseDetailEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

