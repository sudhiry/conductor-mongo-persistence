package com.netflix.conductor.mongo.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.config.ObjectMapperProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@EnableAutoConfiguration
@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = {"com.netflix.conductor.mongo"})
public class TestConfiguration {

    @Bean
    ObjectMapper getObjectMapper() {
        return new ObjectMapperProvider().getObjectMapper();
    }
}
