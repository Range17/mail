package com.range.mail.search;

import com.alibaba.fastjson.JSON;
import com.range.mail.search.config.ElasticSearchConfig;
import lombok.Data;
import lombok.ToString;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
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
     * 构造复杂查询
     */
    @Test
    public void searchData() throws IOException{
        //1、创建检索请求
        SearchRequest searchRequest = new SearchRequest();
        //2、指定索引
        searchRequest.indices("bank");
        //3、指定dsl,检索条件
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //构造检索条件
//        searchSourceBuilder.query();
//        searchSourceBuilder.from();
//        searchSourceBuilder.size();
//        searchSourceBuilder.aggregation();
        searchSourceBuilder.query(QueryBuilders.matchQuery("address", "mill"));

        //按照年龄的值的聚合
        TermsAggregationBuilder ageAgg = AggregationBuilders.terms("ageAgg").field("age").size(10);
        searchSourceBuilder.aggregation(ageAgg);

        //计算平均薪资
        TermsAggregationBuilder balanceAvg = AggregationBuilders.terms("balanceAvg").field("balance");
        searchSourceBuilder.aggregation(balanceAvg);

        //打印检索语句
        System.out.println(searchSourceBuilder.toString());


        searchRequest.source(searchSourceBuilder);

        //4、执行检索
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest,ElasticSearchConfig.COMMON_OPTIONS);

        //5、分析结果
        System.out.println(searchResponse.toString());

        //6、获取所有查到的数据
        SearchHits hits = searchResponse.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit:searchHits) {
            String string = hit.getSourceAsString();
            Account account = JSON.parseObject(string, Account.class);
            System.out.println("account："+account);
        }


        //获取这次检索到的分析信息
        Aggregations aggregations = searchResponse.getAggregations();

        Terms ageAgg1 = aggregations.get("ageAgg");
        for (Terms.Bucket bucket:ageAgg1.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("年龄：" + keyAsString + "==>" + bucket.getDocCount());
        }

//        Avg balanceAvg1 = aggregations.get("balanceAvg");
//        System.out.println("平均薪资："+balanceAvg1.getValue());

    }


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

    @ToString
    @Data
    static class Account{
        private int account_number;
        private int balance;
        private String firstName;
        private String lastName;
        private int age;
        private String gender;
        private String address;
        private String employee;
        private String email;
        private String city;
        private String state;
    }

    @Test
    public void contextLoads() {
        System.out.println(restHighLevelClient);
    }

}
