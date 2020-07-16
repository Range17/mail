package com.range.mail.product.web;

import com.range.mail.product.entity.CategoryEntity;
import com.range.mail.product.service.CategoryService;
import com.range.mail.product.vo.Catelog2Vo;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redissonClient;

    @GetMapping({"/","/index.html"})
    public String indexPage(Model model){
        // TODO 1 查出所有的1级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categories();
//         视图解析器进行拼串
//         classpath:/templates/ + result + .html
        model.addAttribute("categories", categoryEntities);
        return "index";
    }

    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson() throws InterruptedException {
        return categoryService.getCatalogJson();
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello() {
       //1、获取一把锁，只要锁的名字一样，就是同一把锁
        RLock lock = redissonClient.getLock("my-lock");

        //2、加锁
        lock.lock();//阻塞式等待，默认加的锁时间都是30s

        try {
            System.out.println("加锁成功,执行业务..."+Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            System.out.println("释放锁..."+Thread.currentThread().getId());
            lock.unlock();
        }
        return "hello";
    }
}
