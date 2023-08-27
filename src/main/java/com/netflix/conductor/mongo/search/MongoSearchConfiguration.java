package com.netflix.conductor.mongo.search;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * IMPORTANT:
 * There is conflict between Mongo Search vs ElasticSearch
 * For now we are not using Mongo Search, we are expecting to use ElasticSearch
 */

//@Configuration(proxyBeanMethods = false)
//@ConditionalOnProperty(name = "conductor.indexing.type", havingValue = "mongo")
//@Import({MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
//@EnableMongoRepositories(basePackages = {"com.netflix.conductor.mongo.search"})
public class MongoSearchConfiguration {

//    @Bean
//    @Qualifier("MongoSearchRetryTemplate")
//    RetryTemplate getMongoSearchRetryTemplate() {
//        RetryTemplate retryTemplate = new RetryTemplate();
//
//        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
//        fixedBackOffPolicy.setBackOffPeriod(1000l);
//        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
//
//        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
//        retryPolicy.setMaxAttempts(2);
//        retryTemplate.setRetryPolicy(retryPolicy);
//
//        return retryTemplate;
//    }
}
