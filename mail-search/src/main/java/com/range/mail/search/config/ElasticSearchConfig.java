package com.range.mail.search.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ElasticSearchConfig {

    @Bean
    public RestHighLevelClient esRestClient(){
        RestClientBuilder restClientBuilder = null;
        restClientBuilder = RestClient.builder(new HttpHost("112.124.19.119",9200,"http"));
        RestHighLevelClient restHighLevelClient = new RestHighLevelClient(restClientBuilder);
        return restHighLevelClient;

//        RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(new HttpHost("112.124.19.119",9200,"http")));
//        return client;
    }
}
