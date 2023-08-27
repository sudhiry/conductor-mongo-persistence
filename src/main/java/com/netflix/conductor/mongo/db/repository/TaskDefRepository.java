package com.netflix.conductor.mongo.db.repository;

import com.netflix.conductor.mongo.db.models.MetaTaskDef;
import org.springframework.data.mongodb.repository.*;

public interface TaskDefRepository extends MongoRepository<MetaTaskDef, String> {

    MetaTaskDef getFirstByName(String name);

    @ExistsQuery("{ name: ?0 }")
    boolean existsMetaTaskDefByName(String name);

    @Query("{ name: ?0 }")
    @Update("{ $set: { json_data: ?1}}")
    void updateMetaTaskDefByName(String name, String json_data);

    @DeleteQuery("{ name: ?0 }")
    void deleteMetaTaskDefByName(String name);
}
