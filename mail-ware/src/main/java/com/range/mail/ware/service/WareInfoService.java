package com.range.mail.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.range.common.utils.PageUtils;
import com.range.mail.ware.entity.WareInfoEntity;
import com.range.mail.ware.vo.FareVo;

import java.util.Map;

/**
 * 仓库信息
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 17:16:08
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 根据用户的收货地址计算运费
     * @param addrId
     * @retur
     */
    FareVo getFare(Long addrId);
}

