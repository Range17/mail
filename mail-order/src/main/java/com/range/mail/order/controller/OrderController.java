package com.range.mail.order.controller;

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.range.mail.order.entity.OrderEntity;
import com.range.mail.order.service.OrderService;
import com.range.common.utils.PageUtils;
import com.range.common.utils.R;



/**
 * 订单
 *
 * @author range
 * @email range27@gmail.com
 * @date 2020-06-12 23:57:55
 */
@RestController
@RequestMapping("order/order")
public class OrderController {
    @Autowired
    private OrderService orderService;

    /**
     * 查询当前登录用户的所有订单 分页查询订单信息
     * @param params
     * @return
     */
    @RequestMapping("/listWithItems")
    public R listWithItems(@RequestBody Map<String, Object> params) {
        PageUtils page = orderService.queryPageWithItems(params);
        return R.ok().put("page", page);
    }

    /**
     * 获取订单状态
     *
     * @param orderSn
     * @return
     */
    @GetMapping("/status/{orderSn}")
    public R getOrderStatus(@PathVariable("orderSn") String orderSn) {
        OrderEntity orderEntity = orderService.getOrderByOrderSn(orderSn);
        return R.ok().setData(orderEntity);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = orderService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
		OrderEntity order = orderService.getById(id);

        return R.ok().put("order", order);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody OrderEntity order){
		orderService.save(order);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    public R update(@RequestBody OrderEntity order){
		orderService.updateById(order);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
		orderService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
