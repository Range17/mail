package com.range.mail.order.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

public class HelloController {
    @GetMapping("/{page}.html")
    public String page(@PathVariable("page") String page) {
        System.out.println(page);
        return page;
    }

}
