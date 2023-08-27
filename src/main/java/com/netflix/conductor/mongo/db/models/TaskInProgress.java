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
@Document("task_in_progress")
@CompoundIndexes({
        @CompoundIndex(name = "task_in_progress_pk_uk", def = "{'task_def_name' : 1, 'task_id': 1}", unique = true)
})
public class TaskInProgress {

    @Field("task_def_name")
    private String taskDefName;

    @Field("task_type")
    private String taskType;

    @Field("workflow_id")
    private String workflowId;

    @Field("task_id")
    private String taskId;

    @Field("in_progress_status")
    private boolean inProgressStatus;

    @Field("created_on")
    private Date createdOn;

    @Field("modified_on")
    private Date modifiedOn;

}
