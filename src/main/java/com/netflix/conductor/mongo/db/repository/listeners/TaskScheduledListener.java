package com.netflix.conductor.mongo.db.repository.listeners;

import com.netflix.conductor.mongo.db.models.TaskScheduled;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TaskScheduledListener implements BeforeConvertCallback<TaskScheduled> {
    @Override
    public TaskScheduled onBeforeConvert(TaskScheduled entity, String collection) {
        if(entity.getCreatedOn() == null) {
            entity.setCreatedOn(new Date());
        }
        entity.setModifiedOn(new Date());
        return entity;
    }
}
