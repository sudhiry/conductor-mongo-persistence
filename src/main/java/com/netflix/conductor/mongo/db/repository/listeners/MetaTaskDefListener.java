package com.netflix.conductor.mongo.db.repository.listeners;

import com.netflix.conductor.mongo.db.models.MetaTaskDef;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MetaTaskDefListener implements BeforeConvertCallback<MetaTaskDef> {
    @Override
    public MetaTaskDef onBeforeConvert(MetaTaskDef entity, String collection) {
        if(entity.getCreatedOn() == null) {
            entity.setCreatedOn(new Date());
        }
        entity.setModifiedOn(new Date());
        return entity;
    }
}
