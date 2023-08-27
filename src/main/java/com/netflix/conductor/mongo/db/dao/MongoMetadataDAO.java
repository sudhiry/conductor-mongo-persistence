package com.netflix.conductor.mongo.db.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowDef;
import com.netflix.conductor.core.exception.ConflictException;
import com.netflix.conductor.core.exception.NotFoundException;
import com.netflix.conductor.dao.MetadataDAO;
import com.netflix.conductor.mongo.db.models.MetaTaskDef;
import com.netflix.conductor.mongo.db.models.MetaWorkflowDef;
import com.netflix.conductor.mongo.db.repository.TaskDefRepository;
import com.netflix.conductor.mongo.db.repository.WorkflowDefRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class MongoMetadataDAO  extends MongoDBBaseDAO implements MetadataDAO {

    private final TaskDefRepository taskDefRepository;

    private final WorkflowDefRepository workflowDefRepository;

    private final MongoTemplate mongoTemplate;

    public MongoMetadataDAO(
            ObjectMapper objectMapper,
            @Qualifier("MongoDBRetryTemplate") RetryTemplate retryTemplate,
            MongoTemplate mongoTemplate,
            TaskDefRepository taskDefRepository,
            WorkflowDefRepository workflowDefRepository) {
        super(objectMapper, retryTemplate);
        this.mongoTemplate = mongoTemplate;
        this.taskDefRepository = taskDefRepository;
        this.workflowDefRepository = workflowDefRepository;
    }

    @Override
    public TaskDef createTaskDef(TaskDef taskDef) {
        validate(taskDef);
        return save(taskDef);
    }

    @Override
    public TaskDef updateTaskDef(TaskDef taskDef) {
        validate(taskDef);
        return save(taskDef);
    }

    @Override
    public TaskDef getTaskDef(String name) {
        if (!taskDefRepository.existsMetaTaskDefByName(name)) {
            return null;
        }
        MetaTaskDef metaTaskDef = taskDefRepository.getFirstByName(name);
        return readValue(metaTaskDef.getJsonData(), TaskDef.class);
    }

    @Override
    public List<TaskDef> getAllTaskDefs() {
        List<MetaTaskDef> metaTaskDefs = taskDefRepository.findAll();
        if(metaTaskDefs.size() == 0) {
            return Collections.emptyList();
        }
        return metaTaskDefs.stream()
                .map(metaTaskDef -> readValue(metaTaskDef.getJsonData(), TaskDef.class))
                .toList();
    }

    @Override
    public void removeTaskDef(String name) {
        if(!taskDefRepository.existsMetaTaskDefByName(name)){
            throw new NotFoundException("Task Definition with name " + name + " do not exists!");
        }
        taskDefRepository.deleteMetaTaskDefByName(name);
    }

    @Override
    public void createWorkflowDef(WorkflowDef def) {
        validate(def);
        if(workflowDefRepository.existsMetaWorkflowDefByNameAndVersion(def.getName(), def.getVersion())) {
            throw new ConflictException("Workflow Definition with name " + def.getName() + " and version " + def.getVersion() + " already exists!");
        }
        save(def);
    }

    @Override
    public void updateWorkflowDef(WorkflowDef def) {
        validate(def);
        save(def);
    }

    @Override
    public Optional<WorkflowDef> getLatestWorkflowDef(String name) {
        // FIXME revisit for query optimization
        List<MetaWorkflowDef> metaWorkflowDefs = workflowDefRepository.getAllByNameOrderByVersionDesc(name).stream().limit(1).toList();
        if(metaWorkflowDefs.isEmpty()) {
            return Optional.empty();
        }
        WorkflowDef workflowDef = readValue(metaWorkflowDefs.get(0).getJsonData(), WorkflowDef.class);
        return Optional.of(workflowDef);
    }

    @Override
    public Optional<WorkflowDef> getWorkflowDef(String name, int version) {
        MetaWorkflowDef metaWorkflowDef = workflowDefRepository.getFirstByNameAndVersion(name, version);
        if (metaWorkflowDef == null) {
            return Optional.empty();
        }
        return Optional.of(readValue(metaWorkflowDef.getJsonData(), WorkflowDef.class));
    }

    @Override
    public void removeWorkflowDef(String name, Integer version) {
        if(!workflowDefRepository.existsMetaWorkflowDefByNameAndVersion(name, version)) {
            throw new NotFoundException("Workflow Definition with name " + name + " and version " + version + " do not exists!");
        }
        workflowDefRepository.deleteMetaWorkflowDefByNameAndVersion(name, version);
    }

    @Override
    public List<WorkflowDef> getAllWorkflowDefs() {
        List<MetaWorkflowDef> metaWorkflowDefs = workflowDefRepository.findAll();
        if(metaWorkflowDefs.isEmpty()) {
            return Collections.emptyList();
        }
        return metaWorkflowDefs.stream()
            .map(metaWorkflowDef -> readValue(metaWorkflowDef.getJsonData(), WorkflowDef.class))
            .toList();
    }

    @Override
    public List<WorkflowDef> getAllWorkflowDefsLatestVersions() {
        // FIXME revisit for query optimization
        List<String> workflowDefNames = mongoTemplate.findDistinct("name", MetaWorkflowDef.class, String.class);
        return workflowDefNames
                .stream()
                .map(this::getLatestWorkflowDef)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public List<WorkflowDef> getAllVersions(String name) {
        List<MetaWorkflowDef> metaWorkflowDefs = workflowDefRepository.getAllByNameOrderByVersion(name);
        if(metaWorkflowDefs.isEmpty()) {
            return Collections.emptyList();
        }
        return metaWorkflowDefs.stream()
                .map(metaWorkflowDef -> readValue(metaWorkflowDef.getJsonData(), WorkflowDef.class))
                .toList();
    }

    public List<String> findAll() {
        return mongoTemplate.findDistinct("name", MetaWorkflowDef.class, String.class);
    }

    private void validate(TaskDef taskDef) {
        Preconditions.checkNotNull(taskDef, "TaskDef object cannot be null");
        Preconditions.checkNotNull(taskDef.getName(), "TaskDef name cannot be null");
    }

    private TaskDef save(TaskDef taskDef) {
        if(taskDefRepository.existsMetaTaskDefByName(taskDef.getName())) {
            taskDefRepository.updateMetaTaskDefByName(taskDef.getName(), toJson(taskDef));
        } else {
            MetaTaskDef metaTaskDef = new MetaTaskDef();
            metaTaskDef.setName(taskDef.getName());
            metaTaskDef.setJsonData(toJson(taskDef));
            taskDefRepository.insert(metaTaskDef);
        }
        return taskDef;
    }

    private void validate(WorkflowDef def) {
        Preconditions.checkNotNull(def, "WorkflowDef object cannot be null");
        Preconditions.checkNotNull(def.getName(), "WorkflowDef name cannot be null");
    }

    private void save(WorkflowDef def) {
        if(workflowDefRepository.existsMetaWorkflowDefByNameAndVersion(def.getName(), def.getVersion())) {
            workflowDefRepository.updateByNameAndVersion(def.getName(), def.getVersion(), toJson(def));
        } else {
            MetaWorkflowDef metaWorkflowDef = new MetaWorkflowDef();
            metaWorkflowDef.setName(def.getName());
            metaWorkflowDef.setVersion(def.getVersion());
            metaWorkflowDef.setJsonData(toJson(def));
            workflowDefRepository.save(metaWorkflowDef);
        }
    }
}
