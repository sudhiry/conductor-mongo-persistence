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
@Document("queue_message")
@CompoundIndexes({
        @CompoundIndex(name = "queue_message_pk", def = "{'queue_name' : 1, 'message_id': 1}", unique = true),
        @CompoundIndex(name = "combo_queue_message", def = "{'queue_name' : 1, 'priority': 1, 'popped' : 1, 'deliver_on': 1, 'created_on': 1}")
})
public class QueueMessage {

    @Indexed(name = "queue_message_queue_name_idx")
    @Field("queue_name")
    private String queueName;

    @Field("message_id")
    private String messageId;

    @Field("priority")
    private int priority;

    @Field("popped")
    private boolean popped;

    @Field("offset_time_seconds")
    private long offsetTimeSeconds;

    @Field("payload")
    private String payload;

    @Field("created_on")
    private Date createdOn;

    @Field("deliver_on")
    private Date deliverOn;

}
