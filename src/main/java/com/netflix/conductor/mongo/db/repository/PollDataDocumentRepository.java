package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.models.PollDataDocument;
import org.springframework.data.mongodb.repository.ExistsQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.List;

public interface PollDataDocumentRepository extends MongoRepository<PollDataDocument, String> {

    @Query("{ queue_name: ?0, domain: ?1 }")
    @Update("{ $set: { json_data: ?2 } }")
    void updateFirstByQueueNameAndDomain(String queueName, String domain, String jsonData);

    @ExistsQuery("{ queue_name: ?0, domain: ?1 }")
    boolean existsByQueueNameAndDomain(String queueName, String domain);

    PollDataDocument getFirstByQueueNameAndDomain(String queueName, String domain);

    @Query("{ queue_name: ?0 }")
    List<PollDataDocument> getAllByQueueName(String queueName);
}
