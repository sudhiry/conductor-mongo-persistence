package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.models.TaskInProgress;
import org.springframework.data.mongodb.repository.*;

import java.util.List;

public interface TaskInProgressRepository extends MongoRepository<TaskInProgress, String> {

    @Query("{ task_def_name: ?0, workflow_id: ?1 }")
    List<TaskInProgress> getAllByTaskDefNameAndWorkflowId(String taskDefName, String workflowId);

    @Query("{ task_def_name: ?0 }")
    List<TaskInProgress> getTaskInProgressesByTaskDefName(String taskDefName);

    @Query("{ task_type: ?0, workflow_id: ?1 }")
    List<TaskInProgress> getTaskInProgressesByTaskTypeAndWorkflowId(String taskType, String workflowId);

    @Query(value = "{ task_def_name: ?0 }", sort = "{ created_on: 1 }")
    List<TaskInProgress> getAllByTaskDefNameOrderByCreatedOn(String taskDefName);

    @ExistsQuery("{ task_def_name: ?0, task_id: ?1 }")
    boolean existsTaskInProgressByTaskDefNameAndTaskId(String taskDefName, String taskId);

    @Query("{ task_def_name: ?0, task_id: ?1 }")
    @Update("{ $set: { in_progress_status: ?2}}")
    void updateTaskInProgressByTaskDefNameAndTaskId(String taskDefName, String taskId, boolean inProgress);

    @DeleteQuery("{ task_def_name: ?0, task_id: ?1 }")
    void removeTaskInProgressByTaskDefNameAndTaskId(String taskDefName, String taskId);

    @CountQuery("{ task_def_name: ?0, in_progress_status: ?1 }")
    long countTaskInProgressByTaskDefNameAndInProgressStatus(String taskDefName, boolean inProgressStatus);
}
