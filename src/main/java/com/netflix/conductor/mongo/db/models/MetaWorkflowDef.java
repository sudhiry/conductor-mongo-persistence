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
@Document("meta_workflow_def")
@CompoundIndexes({
        @CompoundIndex(name = "meta_workflow_def_pk", def = "{'name' : 1, 'version': 1}", unique = true)
})
public class MetaWorkflowDef {

    @Indexed(name = "meta_workflow_def_name_idx")
    @Field("name")
    private String name;

    @Field("version")
    private int version;

    @Field("json_data")
    private String jsonData;

    @Field("created_on")
    private Date createdOn;

    @Field("modified_on")
    private Date modifiedOn;

}
