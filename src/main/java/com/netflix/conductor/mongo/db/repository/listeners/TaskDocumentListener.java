package com.netflix.conductor.mongo.db.repository.listeners;

import com.netflix.conductor.mongo.db.models.TaskDocument;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TaskDocumentListener implements BeforeConvertCallback<TaskDocument> {

    @Override
    public TaskDocument onBeforeConvert(TaskDocument entity, String collection) {
        if(entity.getCreatedOn() == null) {
            entity.setCreatedOn(new Date());
        }
        return entity;
    }
}
