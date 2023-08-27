package com.netflix.conductor.mongo.db.repository.listeners;

import com.netflix.conductor.mongo.db.models.MetaWorkflowDef;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MetaWorkflowDefListener implements BeforeConvertCallback<MetaWorkflowDef> {
    @Override
    public MetaWorkflowDef onBeforeConvert(MetaWorkflowDef entity, String collection) {
        if(entity.getCreatedOn() == null) {
            entity.setCreatedOn(new Date());
        }
        entity.setModifiedOn(new Date());
        return entity;
    }
}