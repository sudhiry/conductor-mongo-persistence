package com.netflix.conductor.mongo.search.repository;

import com.netflix.conductor.mongo.search.models.TaskExecutionLogs;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface TaskExecutionLogsRepository extends MongoRepository<TaskExecutionLogs, String> {

    @Query(value = "{ task_id: ?0 }", sort = "{ created_time:  1 }")
    List<TaskExecutionLogs> findAllByTaskId(String taskId);
}
