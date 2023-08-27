package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.models.WorkflowDefToWorkflow;
import org.springframework.data.mongodb.repository.DeleteQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface WorkflowDefToWorkflowRepository extends MongoRepository<WorkflowDefToWorkflow, String> {

    @DeleteQuery("{ workflow_def: ?0, date: ?1, workflow_id: ?2 }")
    void removeWorkflowDefToWorkflowByWorkflowDefAndDateStrAndWorkflowId(String workflowDef, Long date, String workflowId);


    @Query("{ workflow_def: ?0, date: { $gte: ?1, $lt: ?2 } }")
    List<WorkflowDefToWorkflow> getByWorkflowDefAndBetweenDate(String workflowDef, Long startTime, Long endTime);

    @Query("{ workflow_def: ?0 }")
    List<WorkflowDefToWorkflow> getByWorkflowDef(String workflowDef);
}
