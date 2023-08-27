package com.netflix.conductor.mongo.db.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("event_execution")
@CompoundIndexes({
        @CompoundIndex(name = "event_execution_pk_uk", def = "{'event_handler_name' : 1, 'message_id': 1, 'execution_id': 1}", unique = true)
})
public class EventExecutionDocument {
    @Indexed(name = "event_execution_event_name_idx")
    @Field("event_name")
    private String eventName;

    @Indexed
    @Field("event_handler_name")
    private String eventHandlerName;

    @Field("message_id")
    private String messageId;

    @Field("execution_id")
    private String executionId;

    @Field("json_data")
    private String jsonData;

    @Field("created_on")
    private Date createdOn;

    @Field("modified_on")
    private Date modifiedOn;

}
