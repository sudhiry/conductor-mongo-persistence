package com.netflix.conductor.mongo.db.repository;


import com.netflix.conductor.mongo.db.models.MetaWorkflowDef;
import org.springframework.data.mongodb.repository.*;

import java.util.List;

public interface WorkflowDefRepository extends MongoRepository<MetaWorkflowDef, String> {

    List<MetaWorkflowDef> getAllByNameOrderByVersionDesc(String name);

    List<MetaWorkflowDef> getAllByNameOrderByVersion(String name);

    MetaWorkflowDef getFirstByNameAndVersion(String name, int version);

    @ExistsQuery("{ name: ?0, version: ?1 }")
    boolean existsMetaWorkflowDefByNameAndVersion(String name, int version);

    @Query("{ name: ?0, version: ?1 }")
    @Update("{ $set: { json_data: ?2}}")
    void updateByNameAndVersion(String name, int version, String json_data);

    @DeleteQuery("{ name: ?0, version: ?1 }")
    void deleteMetaWorkflowDefByNameAndVersion(String name, int version);

}
