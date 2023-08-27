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
@Document("workflow_def_to_workflow")
@CompoundIndexes({
        @CompoundIndex(name = "workflow_def_to_workflow_pk_uk", def = "{'workflow_def' : 1, 'created_on': 1, 'workflow_id': 1}", unique = true)
})
public class WorkflowDefToWorkflow {

    @Indexed(name = "workflow_def_to_workflow_workflow_id_idx")
    @Field("workflow_id")
    private String workflowId;

    @Field("workflow_def")
    private String workflowDef;

    @Field("date")
    private Long date;

    @Field("created_on")
    private Date createdOn;

    @Field("modified_on")
    private Date modifiedOn;

}
