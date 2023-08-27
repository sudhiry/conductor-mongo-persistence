package com.netflix.conductor.mongo.db.dao;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class BaseMongoTest {

    static final MongoDBContainer MONGO_DB_CONTAINER = new MongoDBContainer(DockerImageName.parse("mongo:6.0"))
            .withReuse(false);

    @DynamicPropertySource
    static void mongodbProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO_DB_CONTAINER::getReplicaSetUrl);
    }

    @BeforeAll
    static void setUpBeforeAll() {
        MONGO_DB_CONTAINER.start();
    }

    @AfterAll
    static void tearDownAfterAll() {
        MONGO_DB_CONTAINER.stop();
    }
}
