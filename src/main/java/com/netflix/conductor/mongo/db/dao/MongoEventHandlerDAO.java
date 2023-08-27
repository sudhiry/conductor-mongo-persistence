package com.netflix.conductor.mongo.db.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.netflix.conductor.common.metadata.events.EventHandler;
import com.netflix.conductor.core.exception.ConflictException;
import com.netflix.conductor.core.exception.NotFoundException;
import com.netflix.conductor.dao.EventHandlerDAO;
import com.netflix.conductor.mongo.db.repository.EventHandlerRepository;
import com.netflix.conductor.mongo.db.models.MetaEventHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class MongoEventHandlerDAO extends MongoDBBaseDAO implements EventHandlerDAO {

    private final EventHandlerRepository eventHandlerRepository;

    public MongoEventHandlerDAO(
            ObjectMapper objectMapper,
            @Qualifier("MongoDBRetryTemplate") RetryTemplate retryTemplate,
            EventHandlerRepository eventHandlerRepository) {
        super(objectMapper, retryTemplate);
        this.eventHandlerRepository = eventHandlerRepository;
    }

//    public EventHandler getEventHandler(String name) {
//        Preconditions.checkNotNull(name, "EventHandler name cannot be null");
//        if (!eventHandlerRepository.existsEventHandlerByName(name)) {
//            return null;
//        }
//        MetaEventHandler metaEventHandler = eventHandlerRepository.getEventHandlerByName(name);
//        return readValue(metaEventHandler.getJson_data(), EventHandler.class);
//    }

    @Override
    public void addEventHandler(EventHandler eventHandler) {
        Preconditions.checkNotNull(eventHandler.getName(), "EventHandler name cannot be null");
        if (eventHandlerRepository.existsByName(eventHandler.getName())) {
            throw new ConflictException("EventHandler with name " + eventHandler.getName() + " already exists!");
        }
        MetaEventHandler metaEventHandler = new MetaEventHandler();
        metaEventHandler.setName(eventHandler.getName());
        metaEventHandler.setEvent(eventHandler.getEvent());
        metaEventHandler.setActive(eventHandler.isActive());
        metaEventHandler.setJsonData(toJson(eventHandler));
        eventHandlerRepository.insert(metaEventHandler);
    }

    @Override
    public void updateEventHandler(EventHandler eventHandler) {
        Preconditions.checkNotNull(eventHandler.getName(), "EventHandler name cannot be null");
        if (!eventHandlerRepository.existsByName(eventHandler.getName())) {
            throw new NotFoundException("EventHandler with name " + eventHandler.getName() + " doesn't exists!");
        }
        eventHandlerRepository.updateEventHandlerByName(eventHandler.getName(), eventHandler.getEvent(), eventHandler.isActive(), toJson(eventHandler));
    }

    @Override
    public void removeEventHandler(String name) {
        Preconditions.checkNotNull(name, "EventHandler name cannot be null");
        if (!eventHandlerRepository.existsByName(name)) {
            throw new NotFoundException("EventHandler with name " + name + " doesn't exists!");
        }
        eventHandlerRepository.deleteEventHandlerByName(name);
    }

    @Override
    public List<EventHandler> getAllEventHandlers() {
        List<MetaEventHandler> metaEventHandlers = eventHandlerRepository.findAll();
        if (metaEventHandlers.size() == 0) {
            return Collections.emptyList();
        }
        return metaEventHandlers.stream()
            .map(metaEventHandler -> readValue(metaEventHandler.getJsonData(), EventHandler.class))
            .toList();
    }

    @Override
    public List<EventHandler> getEventHandlersForEvent(String event, boolean activeOnly) {
        List<MetaEventHandler> metaEventHandlers = eventHandlerRepository.getEventHandlersByEventAndActive(event, activeOnly);
        if (metaEventHandlers.size() == 0) {
            return Collections.emptyList();
        }
        return metaEventHandlers.stream()
            .map(metaEventHandler -> readValue(metaEventHandler.getJsonData(), EventHandler.class))
            .toList();
    }

}
