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
@Document("poll_data")
@CompoundIndexes({
        @CompoundIndex(name = "poll_data_pk_uk", def = "{'queue_name' : 1, 'domain': 1}", unique = true)
})
public class PollDataDocument {

    @Indexed(name = "poll_data_queue_name_idx")
    @Field("queue_name")
    private String queueName;

    @Field("domain")
    private String domain;

    @Field("json_data")
    private String jsonData;

    @Field("created_on")
    private Date createdOn;

    @Field("modified_on")
    private Date modifiedOn;

}
