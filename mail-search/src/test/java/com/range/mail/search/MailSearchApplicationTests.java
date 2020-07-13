package com.range.mail.search;

import com.alibaba.fastjson.JSON;
import com.range.mail.search.config.ElasticSearchConfig;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
public class MailSearchApplicationTests {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 测试存储数据到es
     */
    @Test
    public void indexData() throws IOException {
        IndexRequest indexRequest = new IndexRequest("user");
        //数据的id
        indexRequest.id("1");
        // 1、insert data
        //        indexRequest.source("userName","range","age","23","gender","男");

        //2、insert data
        User user = new User();
        String jsonString = JSON.toJSONString(user);
        //要保存的内容
        indexRequest.source(jsonString, XContentType.JSON);

        //执行操作
        IndexResponse index = restHighLevelClient.index(indexRequest, ElasticSearchConfig.COMMON_OPTIONS);

        //提取响应数据
        System.out.println(index);

    }

    @Data
    class User{
        private String userName;
        private String gender;
        private Integer age;
    }

    @Test
    public void contextLoads() {
        System.out.println(restHighLevelClient);
    }

}
