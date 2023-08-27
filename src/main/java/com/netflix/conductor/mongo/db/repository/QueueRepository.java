package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.models.QueueDocument;
import org.springframework.data.mongodb.repository.ExistsQuery;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QueueRepository extends MongoRepository<QueueDocument, String> {

    @ExistsQuery("{ name: ?0 }")
    boolean existsByName(String queueName);

}
