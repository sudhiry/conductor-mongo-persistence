package com.netflix.conductor.mongo.db.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document("meta_event_handler")
public class MetaEventHandler {

    @Field("name")
    @Indexed(name = "meta_event_handler_name_idx", unique = true)
    private String name;

    @Indexed
    @Field("event")
    private String event;

    @Field("active")
    private Boolean active;

    @Field("json_data")
    private String jsonData;

    @Field("created_on")
    private Date createdOn;

    @Field("modified_on")
    private Date modifiedOn;

}
