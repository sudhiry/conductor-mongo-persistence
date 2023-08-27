package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.models.WorkflowToTask;
import org.springframework.data.mongodb.repository.DeleteQuery;
import org.springframework.data.mongodb.repository.ExistsQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface WorkflowToTaskRepository  extends MongoRepository<WorkflowToTask, String> {

    @Query("{ workflow_id: ?0 }")
    List<WorkflowToTask> getWorkflowToTasksByWorkflowId(String workflowId);

    @ExistsQuery("{ workflow_id: ?0, task_id: ?1 }")
    boolean existsWorkflowToTaskByWorkflowIdAndTaskId(String workflowId, String taskId);

    @DeleteQuery("{ workflow_id: ?0, task_id: ?1 }")
    void removeWorkflowToTaskByWorkflowIdAndTaskId(String workflowId, String taskId);
}
