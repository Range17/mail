package com.range.mail.mailauthserver.controller;

import com.alibaba.fastjson.TypeReference;
import com.range.common.constant.AuthServerConstant;
import com.range.common.exception.BizCodeEnum;
import com.range.common.utils.R;
import com.range.mail.mailauthserver.feign.MemberFeign;
import com.range.mail.mailauthserver.feign.ThirdPartyFeign;
import com.range.mail.mailauthserver.vo.MemberResponseVo;
import com.range.mail.mailauthserver.vo.UserLoginVo;
import com.range.mail.mailauthserver.vo.UserRegisterVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


    @Controller
    public class IndexController {

        @Resource
        private ThirdPartyFeign thirdPartyFeign;

        @Resource
        private StringRedisTemplate stringRedisTemplate;

        @Resource
        private MemberFeign memberFeign;

        @GetMapping("/sms/send")
        @ResponseBody
        public R sendSMS(@RequestParam("phone") String phone) {

            String redisCode = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone);
            if (!StringUtils.isEmpty(redisCode)) {
                long l = Long.parseLong(redisCode.split("_")[1]);
                if (System.currentTimeMillis() - l < 60000) {
                    return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
                }
            }

            String code = UUID.randomUUID().toString().substring(0, 5);
            String redisStorage = code + "_" + System.currentTimeMillis();
            // 为验证码设置过期时间
            stringRedisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, redisStorage, 10, TimeUnit.MINUTES);

            thirdPartyFeign.sendSMSCode(phone, code);
            return R.ok();
        }

        @PostMapping("/reg")
        public String register(@Valid UserRegisterVo Vo, BindingResult result, RedirectAttributes redirectAttributes) {
            if (result.hasErrors()) {
                Map<String, String> errors = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage));
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.catmall.com/register.html";
            }

            // 校验验证码
            String code = Vo.getCode();
            String s = stringRedisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + Vo.getPhone());
            if (!StringUtils.isEmpty(s)) {
                if (code.equals(s.split("_")[0])) {
                    stringRedisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + Vo.getPhone());
                    R register = memberFeign.register(Vo);
                    if (register.getCode() == 0) {
                        return "redirect:http://auth.catmall.com/login.html";
                    } else {
                        Map<String, String> errors = new HashMap<>();
                        errors.put("msg", (String) register.getData("msg", new TypeReference<String>(){}));
                        redirectAttributes.addFlashAttribute("errors", errors);
                        return "redirect:http://auth.catmall.com/register.html";
                    }
                } else {
                    Map<String, String> errors = new HashMap<>();
                    errors.put("code", "验证码错误");
                    redirectAttributes.addFlashAttribute("errors", errors);
                    return "redirect:http://auth.catmall.com/register.html";
                }
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("code", "验证码错误");
                redirectAttributes.addFlashAttribute("errors", errors);
                return "redirect:http://auth.catmall.com/register.html";
            }
        }

        @GetMapping("/login.html")
        public String loginPage(HttpSession session) {
            Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
            return !ObjectUtils.isEmpty(attribute) ? "redirect:http://catmall.com" : "login";
        }

        @PostMapping("/login")
        public String login(UserLoginVo userLoginVo, RedirectAttributes redirectAttributes,
                            HttpSession session) {
            R login = memberFeign.login(userLoginVo);
            if (login.getCode() != 0) {
                MemberResponseVo loginUser = (MemberResponseVo) login.getData(new TypeReference<MemberResponseVo>() {});
                session.setAttribute(AuthServerConstant.LOGIN_USER, loginUser);
                return "redirect:http://catmall.com";
            } else {
                Map<String, String> errors = new HashMap<>();
                errors.put("msg", (String) login.getData("msg", new TypeReference<String>(){}));
                redirectAttributes.addAttribute("errors", errors);
                return "redirect:http://auth.catmall.com/login.html";
            }
        }

    }

