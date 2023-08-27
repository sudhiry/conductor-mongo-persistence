package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.models.WorkflowPending;
import org.springframework.data.mongodb.repository.*;

import java.util.List;

public interface WorkflowPendingRepository extends MongoRepository<WorkflowPending, String> {

    @DeleteQuery("{ workflow_type: ?0, workflow_id: ?1 }")
    void deleteWorkflowPendingByWorkflowTypeAndWorkflowId(String workflowType, String workflowId);

    @ExistsQuery("{ workflow_type: ?0, workflow_id: ?1 }")
    boolean existsWorkflowPendingByWorkflowTypeAndWorkflowId(String workflowType, String workflowId);

    @Query("{ workflow_type: ?0 }")
    List<WorkflowPending> getWorkflowPendingsByWorkflowType(String workflowType);

    @CountQuery("{ workflow_type: ?0 }")
    long countWorkflowPendingsByWorkflowType(String workflowType);
}
