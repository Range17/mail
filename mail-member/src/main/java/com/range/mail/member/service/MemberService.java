package com.range.mail.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.range.common.utils.PageUtils;
import com.range.mail.member.entity.MemberEntity;
import com.range.mail.member.vo.MemberLoginVo;
import com.range.mail.member.vo.MemberRegisterVo;
import com.range.mail.member.vo.SocialUser;

import java.util.Map;

/**
 * 会员
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 23:54:10
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void register(MemberRegisterVo memberReisterVO);

    MemberEntity login(MemberLoginVo memberLoginVO);


    MemberEntity login(SocialUser socialUser) throws Exception;
}

