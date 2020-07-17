package com.range.mail.product.web;

import com.range.mail.product.entity.CategoryEntity;
import com.range.mail.product.service.CategoryService;
import com.range.mail.product.vo.Catelog2Vo;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

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

    @GetMapping("/hello")
    @ResponseBody
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

    /**
     *   修改期间：写锁是一个排它锁，读锁是一个共享锁（写锁没释放就必须等待）
     *     读+读：并发锁，所有的读锁同时加锁成功，会在redis中记录好
     *     写+读：等待写锁释放
     *     写+写：阻塞方式
     *     读+写：有读锁，写也需要等待
     *     只要有写的存在，就必须等待
     */

    @GetMapping("/write")
    @ResponseBody
    public String writeValue(){
        RReadWriteLock lock = redissonClient.getReadWriteLock("rw-lock");
        String s = "";
        RLock rLock = lock.writeLock();
        try {
            rLock.lock();
            System.out.println("写锁加锁成功..."+Thread.currentThread().getId());
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            rLock.unlock();
            System.out.println("写锁释放..."+Thread.currentThread().getId());
        }
        return s;
    }

    @GetMapping("/read")
    @ResponseBody
    public String readValue(){
        RReadWriteLock lock = redissonClient.getReadWriteLock("rw-lock");
        String s = "";
        RLock rLock = lock.readLock();
        try {
            rLock.lock();
            System.out.println("读锁加锁成功..."+Thread.currentThread().getId());
            s = stringRedisTemplate.opsForValue().get("writeValue");
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            rLock.unlock();
            System.out.println("读锁释放..."+Thread.currentThread().getId());
        }
        return s;
    }


    /**
     * 信号量演示
     * 车库停车场景、来了一辆车就占用一个，走了一个就释放一个
     * 也可以做限流方案
     */
    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
//        park.acquire();//获取一个信号，占一个车位  //阻塞试
        Boolean b = park.tryAcquire();//尝试获取  //非阻塞
        if(b){
            //执行业务
        }else {
            return "error,当前无车位";
        }
        return "ok";
    }
    @GetMapping("/go")
    @ResponseBody
    public String go() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        park.release();//释放一个车位
        return "ok";
    }


    /**
     * 闭锁演示
     * @return
     * @throws InterruptedException
     */
    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.trySetCount(5);
        door.await();//等待闭锁都完成
        return "闭锁完成";
    }

    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id){
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.countDown();
        return "第" + id + "个走了";
    }
}
