package com.range.mail.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.range.common.utils.R;
import com.range.mail.ware.feign.MemberFeignService;
import com.range.mail.ware.vo.FareVo;
import com.range.mail.ware.vo.MemberAddressVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.range.common.utils.PageUtils;
import com.range.common.utils.Query;

import com.range.mail.ware.dao.WareInfoDao;
import com.range.mail.ware.entity.WareInfoEntity;
import com.range.mail.ware.service.WareInfoService;
import org.springframework.util.ObjectUtils;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {


    @Autowired
    MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        QueryWrapper<WareInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)){
            wrapper.eq("id",key).or().like("name",key).
                    or().like("address",key)
                    .or().like("areacode",key);
        }
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public FareVo getFare(Long attrId) {
        FareVo fareVo = new FareVo();
        R r = memberFeignService.addrInfo(attrId);
        MemberAddressVo data = (MemberAddressVo) r.getData("memberReceiveAddress", new TypeReference<MemberAddressVo>() {});
        if (!ObjectUtils.isEmpty(data)) {
            String phone = data.getPhone();
            String substring = phone.substring(phone.length() - 1);
            fareVo.setFare(new BigDecimal(substring));
            fareVo.setMemberAddressVo(data);
            return fareVo;
        }
        return null;
    }

}