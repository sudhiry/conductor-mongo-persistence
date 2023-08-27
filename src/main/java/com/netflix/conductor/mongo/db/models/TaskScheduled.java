package com.netflix.conductor.mongo.db.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("task_scheduled")
@CompoundIndexes({
        @CompoundIndex(name = "task_scheduled_pk_uk", def = "{'workflow_id' : 1, 'task_key': 1}", unique = true)
})
public class TaskScheduled {

    @Field("workflow_id")
    private String workflowId;

    @Field("task_key")
    private String taskKey;

    @Field("task_id")
    private String taskId;

    @Field("created_on")
    private Date createdOn;

    @Field("modified_on")
    private Date modifiedOn;

}
