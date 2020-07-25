package com.range.thirdparty.controller;

import com.range.common.utils.R;
import com.range.thirdparty.component.SMSComponent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RequestMapping("/sms")
@RestController
public class SMSController {

    @Resource
    private SMSComponent smsComponent;

    @GetMapping("/send")
    public R sendSMSCode(@RequestParam("phone") String phone, @RequestParam("code") String code) {
        smsComponent.sendSMSCode(phone, code);
        return R.ok();
    }
}