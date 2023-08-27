package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.models.WorkflowDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.List;

public interface WorkflowRepository extends MongoRepository<WorkflowDocument, String> {

    @Query("{ workflow_id: ?0 }")
    @Update("{ $set: { json_data: ?1 } }")
    void updateWorkflowByWorkflowId(String workflowId, String json_data);

    WorkflowDocument getFirstByWorkflowId(String workflowId);

    @Query("{ workflow_id: { $in:  ?0 }, correlation_id: ?1 }")
    List<WorkflowDocument> getByWorkflowIdsAndCorrelationId(List<String> workflowIds, String correlationId);
}
