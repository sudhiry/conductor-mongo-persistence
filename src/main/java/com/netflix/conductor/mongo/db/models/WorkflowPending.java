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
@Document("workflow_pending")
@CompoundIndexes({
        @CompoundIndex(name = "workflow_pending_pk_uk", def = "{'workflow_type' : 1, 'workflow_id': 1}", unique = true)
})
public class WorkflowPending {

    @Indexed(name = "workflow_pending_workflow_type_idx")
    @Field("workflow_type")
    private String workflowType;

    @Field("workflow_id")
    private String workflowId;

    @Field("created_on")
    private Date createdOn;

    @Field("modified_on")
    private Date modifiedOn;

}
