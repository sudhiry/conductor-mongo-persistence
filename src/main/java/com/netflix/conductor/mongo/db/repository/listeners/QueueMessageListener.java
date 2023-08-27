package com.netflix.conductor.mongo.db.repository.listeners;

import com.netflix.conductor.mongo.db.models.QueueMessage;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class QueueMessageListener implements BeforeConvertCallback<QueueMessage> {

    @Override
    public QueueMessage onBeforeConvert(QueueMessage entity, String collection) {
        if(entity.getCreatedOn() == null) {
            entity.setCreatedOn(new Date());
        }
        return entity;
    }
}