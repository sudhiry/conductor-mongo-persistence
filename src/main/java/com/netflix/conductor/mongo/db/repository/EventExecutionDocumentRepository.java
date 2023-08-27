package com.netflix.conductor.mongo.db.repository;


import com.netflix.conductor.mongo.db.models.EventExecutionDocument;
import org.springframework.data.mongodb.repository.DeleteQuery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

public interface EventExecutionDocumentRepository extends MongoRepository<EventExecutionDocument, String> {

    @Query("{ event_handler_name: ?0, event_name: ?1, message_id: ?2, execution_id: ?3}")
    EventExecutionDocument getFirstBy(String eventHandlerName, String eventName, String messageId, String executionId);

    @Query("{ event_handler_name: ?0, event_name: ?1, message_id: ?2, execution_id: ?3}")
    @Update("{ $set: { json_data: ?4 } }")
    void updateEventExecutionDocument(String eventHandlerName, String eventName, String messageId, String executionId, String jsonData);

    @DeleteQuery("{ event_handler_name: ?0, event_name: ?1, message_id: ?2, execution_id: ?3}")
    void deleteEventExecutionDocument(String eventHandlerName, String eventName, String messageId, String executionId);
}
