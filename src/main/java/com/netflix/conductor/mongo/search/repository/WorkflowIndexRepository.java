package com.netflix.conductor.mongo.search.repository;

import com.netflix.conductor.mongo.search.models.WorkflowIndex;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WorkflowIndexRepository extends MongoRepository<WorkflowIndex, String> {

}
