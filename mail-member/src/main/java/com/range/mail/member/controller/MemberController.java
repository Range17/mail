package com.range.mail.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.range.common.exception.BizCodeEnum;
import com.range.mail.member.exception.PhoneExistException;
import com.range.mail.member.exception.UsernameExistException;
import com.range.mail.member.feign.CoupoFeignService;
import com.range.mail.member.vo.MemberLoginVo;
import com.range.mail.member.vo.MemberRegisterVo;
import com.range.mail.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import com.range.mail.member.entity.MemberEntity;
import com.range.mail.member.service.MemberService;
import com.range.common.utils.PageUtils;
import com.range.common.utils.R;



/**
 * 会员
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 23:54:10
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Autowired
    private CoupoFeignService coupoFeignService;


    @PostMapping("/register")
    public R register(@RequestBody MemberRegisterVo memberRegisterVO) {
        try {
            memberService.register(memberRegisterVO);
        } catch (UsernameExistException e) {
            return R.error(BizCodeEnum.USERNAME_EXIST_EXCEPTION.getCode(), BizCodeEnum.USERNAME_EXIST_EXCEPTION.getMsg());
        } catch (PhoneExistException e) {
            return R.error(BizCodeEnum.PHONE_EXIST_EXCEPTION.getCode(), BizCodeEnum.PHONE_EXIST_EXCEPTION.getMsg());
        }
        return R.ok();
    }

    @PostMapping("/oauth2/login")
    public R oauthLogin(@RequestBody SocialUser socialUser) throws Exception {
        MemberEntity memberEntity = memberService.login(socialUser);
        if (!ObjectUtils.isEmpty(memberEntity))
            return R.ok().setData(memberEntity);
        else
            return R.error(BizCodeEnum.LOGIN_EXCEPTION.getCode(), BizCodeEnum.LOGIN_EXCEPTION.getMsg());
    }

    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo memberLoginVO) {
        MemberEntity memberEntity = memberService.login(memberLoginVO);
        if (!ObjectUtils.isEmpty(memberEntity)) {
            return R.ok().setData(memberEntity);
        } else {
            return R.error(BizCodeEnum.LOGIN_EXCEPTION.getCode(), BizCodeEnum.LOGIN_EXCEPTION.getMsg());
        }
    }

    @GetMapping("/coupon")
    public R test(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("range");
        R memberCoupons = coupoFeignService.menberCoupons();
        return R.ok().put("member",memberEntity).put("coupons",memberCoupons.get("coupons"));
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
