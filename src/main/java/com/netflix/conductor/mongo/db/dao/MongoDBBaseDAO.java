package com.netflix.conductor.mongo.db.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;

import java.io.IOException;

public abstract class MongoDBBaseDAO {

    private final ObjectMapper objectMapper;

    protected final RetryTemplate retryTemplate;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public MongoDBBaseDAO(
            ObjectMapper objectMapper,
            RetryTemplate retryTemplate) {
        super();
        this.retryTemplate = retryTemplate;
        this.objectMapper = objectMapper;
    }


    protected String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T readValue(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


//    <R> R getWithRetriedTransactions(final TransactionalFunction<R> function) {
//        try {
//            return retryTemplate.execute(context -> getWithTransaction(function));
//        } catch (Exception e) {
//            throw new NonTransientException(e.getMessage(), e);
//        }
//    }
}
