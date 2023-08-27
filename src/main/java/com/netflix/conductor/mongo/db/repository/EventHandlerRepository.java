package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.models.MetaEventHandler;
import org.springframework.data.mongodb.repository.DeleteQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.util.List;

public interface EventHandlerRepository extends MongoRepository<MetaEventHandler, String> {

    MetaEventHandler getFirstByName(String name);

    boolean existsByName(String name);

    @DeleteQuery("{ name: ?0 }")
    void deleteEventHandlerByName(String name);

    @Query("{ event: ?0, active: ?1 }")
    List<MetaEventHandler> getEventHandlersByEventAndActive(String event, boolean active);

    @Query("{ name: ?0 }")
    @Update("{ $set: { event:  ?1, active: ?2, json_data: ?3}}")
    void updateEventHandlerByName(String name, String event, Boolean active, String json_data);
}
