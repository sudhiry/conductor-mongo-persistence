package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.models.TaskDocument;
import org.springframework.data.mongodb.repository.*;

import java.util.List;

public interface TaskRepository extends MongoRepository<TaskDocument, String> {

    @Query("{ task_id: { $in: ?0 } }")
    List<TaskDocument> getTasksByTaskId(List<String> taskIds);

    @Query("{ task_id: ?0 }")
    TaskDocument getTaskByTaskId(String taskId);

    @ExistsQuery("{ task_id: ?0 }")
    boolean existsTaskByTaskId(String taskId);

    @Query("{ task_id: ?0 }")
    @Update("{ $set: { json_data: ?1 } }")
    void updateTaskByTaskId(String taskId, String json_data);

    @DeleteQuery("{ task_id: ?0 }")
    void removeTaskByTaskId(String taskId);

}
