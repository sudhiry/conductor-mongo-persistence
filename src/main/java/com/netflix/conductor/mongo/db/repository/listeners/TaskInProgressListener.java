package com.netflix.conductor.mongo.db.repository.listeners;

import com.netflix.conductor.mongo.db.models.TaskInProgress;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TaskInProgressListener implements BeforeConvertCallback<TaskInProgress> {

    @Override
    public TaskInProgress onBeforeConvert(TaskInProgress entity, String collection) {
        if(entity.getCreatedOn() == null) {
            entity.setCreatedOn(new Date());
        }
        return entity;
    }
}
