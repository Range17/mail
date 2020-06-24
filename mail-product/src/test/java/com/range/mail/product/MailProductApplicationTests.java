package com.range.mail.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.range.mail.product.entity.BrandEntity;
import com.range.mail.product.service.AttrGroupService;
import com.range.mail.product.service.BrandService;
import com.range.mail.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
@Slf4j
class MailProductApplicationTests {


    @Autowired
    BrandService brandService;

    @Autowired
    CategoryService categoryService;

    @Autowired
    AttrGroupService attrGroupService;

    @Test
    public void findPath(){
        Long[] path = categoryService.findCateLogPath(225L);
        log.info("path:{}", Arrays.asList(path));
    }

    @Test
    void contextLoads() {
//        BrandEntity brandEntity = new BrandEntity();
//        brandEntity.setName("apple");
//        brandService.save(brandEntity);
//        System.out.print("保存成功");


        //mybatisplus支持的查询，有点像spring提供的那个
        //利用wraaper进行构造数据
//        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id",1));
//        System.out.print("保存成功");
//        System.out.print(list.get(0).getName());



    }

}
