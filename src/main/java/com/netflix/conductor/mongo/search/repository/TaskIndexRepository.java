package com.netflix.conductor.mongo.search.repository;

import com.netflix.conductor.mongo.search.models.TaskIndex;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TaskIndexRepository extends MongoRepository<TaskIndex, String> {

}
