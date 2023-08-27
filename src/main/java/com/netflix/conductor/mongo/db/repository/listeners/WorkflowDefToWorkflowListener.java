package com.netflix.conductor.mongo.db.repository.listeners;

import com.netflix.conductor.mongo.db.models.WorkflowDefToWorkflow;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class WorkflowDefToWorkflowListener implements BeforeConvertCallback<WorkflowDefToWorkflow> {
    @Override
    public WorkflowDefToWorkflow onBeforeConvert(WorkflowDefToWorkflow entity, String collection) {
        if(entity.getCreatedOn() == null) {
            entity.setCreatedOn(new Date());
        }
        entity.setModifiedOn(new Date());
        return entity;
    }
}
