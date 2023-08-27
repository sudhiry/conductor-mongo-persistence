package com.netflix.conductor.mongo.db.dao;

import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.core.exception.ConflictException;
import com.netflix.conductor.core.exception.NotFoundException;
import com.netflix.conductor.mongo.db.TestConfiguration;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration(classes = {TestConfiguration.class})
@DataMongoTest
class MongoMetadataDAOTest extends BaseMongoTest {

    @Autowired
    private MongoMetadataDAO mongoMetadataDAO;

    @Test
    public void testDuplicateWorkflowDef() {
        WorkflowDef def = new WorkflowDef();
        def.setName("testDuplicate");
        def.setVersion(1);
        mongoMetadataDAO.createWorkflowDef(def);
        ConflictException conflictException = assertThrows(ConflictException.class, () -> {
            WorkflowDef newDef = new WorkflowDef();
            newDef.setName("testDuplicate");
            newDef.setVersion(1);
            mongoMetadataDAO.createWorkflowDef(newDef);
        }, "ConflictException was expected");
        assertThat(conflictException.getMessage())
                .isEqualTo("Workflow Definition with name testDuplicate and version 1 already exists!");
    }

    @Test
    public void testRemoveNotExistingWorkflowDef() {
        NotFoundException conflictException = assertThrows(NotFoundException.class, () -> {
            mongoMetadataDAO.removeWorkflowDef("test", 1);
        });
        assertThat(conflictException.getMessage())
                .isEqualTo("Workflow Definition with name test and version 1 do not exists!");
    }

    @Test
    public void testWorkflowDefOperations() {
        WorkflowDef def = new WorkflowDef();
        def.setName("workflow-def-1");
        def.setVersion(1);
        def.setDescription("description");
        def.setCreatedBy("unit_test");
        def.setCreateTime(1L);
        def.setOwnerApp("ownerApp");
        def.setUpdatedBy("unit_test2");
        def.setUpdateTime(2L);
        mongoMetadataDAO.createWorkflowDef(def);

        List<WorkflowDef> all = mongoMetadataDAO.getAllWorkflowDefs();
        assertNotNull(all);
        assertEquals(1, all.size());
        assertEquals("workflow-def-1", all.get(0).getName());
        assertEquals(1, all.get(0).getVersion());

        WorkflowDef found = mongoMetadataDAO.getWorkflowDef("workflow-def-1", 1).get();
        assertTrue(EqualsBuilder.reflectionEquals(def, found));

        def.setVersion(3);
        mongoMetadataDAO.createWorkflowDef(def);

        all = mongoMetadataDAO.getAllWorkflowDefs();
        assertNotNull(all);
        assertEquals(2, all.size());
        assertEquals("workflow-def-1", all.get(0).getName());
        assertEquals(1, all.get(0).getVersion());

        found = mongoMetadataDAO.getLatestWorkflowDef(def.getName()).get();
        assertEquals(def.getName(), found.getName());
        assertEquals(def.getVersion(), found.getVersion());
        assertEquals(3, found.getVersion());

        all = mongoMetadataDAO.getAllWorkflowDefsLatestVersions();
        assertNotNull(all);
        assertEquals(1, all.size());
        assertEquals("workflow-def-1", all.get(0).getName());
        assertEquals(3, all.get(0).getVersion());

        all = mongoMetadataDAO.getAllVersions(def.getName());
        assertNotNull(all);
        assertEquals(2, all.size());
        assertEquals("workflow-def-1", all.get(0).getName());
        assertEquals("workflow-def-1", all.get(1).getName());
        assertEquals(1, all.get(0).getVersion());
        assertEquals(3, all.get(1).getVersion());

        def.setDescription("updated");
        mongoMetadataDAO.updateWorkflowDef(def);
        found = mongoMetadataDAO.getWorkflowDef(def.getName(), def.getVersion()).get();
        assertEquals(def.getDescription(), found.getDescription());

        List<String> allnames = mongoMetadataDAO.findAll();
        assertNotNull(allnames);
        assertEquals(1, allnames.size());
        assertEquals(def.getName(), allnames.get(0));

        def.setVersion(2);
        mongoMetadataDAO.createWorkflowDef(def);

        found = mongoMetadataDAO.getLatestWorkflowDef(def.getName()).get();
        assertEquals(def.getName(), found.getName());
        assertEquals(3, found.getVersion());

        mongoMetadataDAO.removeWorkflowDef("workflow-def-1", 3);
        Optional<WorkflowDef> deleted = mongoMetadataDAO.getWorkflowDef("workflow-def-1", 3);
        assertFalse(deleted.isPresent());

        found = mongoMetadataDAO.getLatestWorkflowDef(def.getName()).get();
        assertEquals(def.getName(), found.getName());
        assertEquals(2, found.getVersion());

        mongoMetadataDAO.removeWorkflowDef("workflow-def-1", 1);
        deleted = mongoMetadataDAO.getWorkflowDef("workflow-def-1", 1);
        assertFalse(deleted.isPresent());

        found = mongoMetadataDAO.getLatestWorkflowDef(def.getName()).get();
        assertEquals(def.getName(), found.getName());
        assertEquals(2, found.getVersion());
    }

    @Test
    public void testTaskDefOperations() throws Exception {
        TaskDef def = new TaskDef("taskA");
        def.setDescription("description");
        def.setCreatedBy("unit_test");
        def.setCreateTime(1L);
        def.setInputKeys(Arrays.asList("a", "b", "c"));
        def.setOutputKeys(Arrays.asList("01", "o2"));
        def.setOwnerApp("ownerApp");
        def.setRetryCount(3);
        def.setRetryDelaySeconds(100);
        def.setRetryLogic(TaskDef.RetryLogic.FIXED);
        def.setTimeoutPolicy(TaskDef.TimeoutPolicy.ALERT_ONLY);
        def.setUpdatedBy("unit_test2");
        def.setUpdateTime(2L);

        mongoMetadataDAO.createTaskDef(def);

        TaskDef found = mongoMetadataDAO.getTaskDef(def.getName());
        assertThat(found).isEqualTo(def);

        def.setDescription("updated description");
        mongoMetadataDAO.updateTaskDef(def);
        found = mongoMetadataDAO.getTaskDef(def.getName());
        assertThat(found).isEqualTo(def);
        assertEquals("updated description", found.getDescription());

        for (int i = 0; i < 9; i++) {
            TaskDef tdf = new TaskDef("taskA" + i);
            mongoMetadataDAO.createTaskDef(tdf);
        }

        List<TaskDef> all = mongoMetadataDAO.getAllTaskDefs();
        assertNotNull(all);
        assertEquals(10, all.size());
        Set<String> allnames = all.stream().map(TaskDef::getName).collect(Collectors.toSet());
        assertEquals(10, allnames.size());
        List<String> sorted = allnames.stream().sorted().toList();
        assertEquals(def.getName(), sorted.get(0));

        for (int i = 0; i < 9; i++) {
            assertEquals(def.getName() + i, sorted.get(i + 1));
        }

        for (int i = 0; i < 9; i++) {
            mongoMetadataDAO.removeTaskDef(def.getName() + i);
        }
        all = mongoMetadataDAO.getAllTaskDefs();
        assertNotNull(all);
        assertEquals(1, all.size());
        assertEquals(def.getName(), all.get(0).getName());
    }

    @Test
    public void testRemoveNotExistingTaskDef() {
        String taskDefName = "test" + UUID.randomUUID();
        NotFoundException conflictException = assertThrows(NotFoundException.class, () -> {
            mongoMetadataDAO.removeTaskDef(taskDefName);
        });
        assertThat(conflictException.getMessage())
                .isEqualTo("Task Definition with name " + taskDefName+ " do not exists!");
    }
}