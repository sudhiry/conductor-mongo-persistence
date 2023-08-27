package com.netflix.conductor.mongo.search.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.conductor.common.metadata.events.EventExecution;
import com.netflix.conductor.common.metadata.tasks.TaskExecLog;
import com.netflix.conductor.common.run.SearchResult;
import com.netflix.conductor.common.run.TaskSummary;
import com.netflix.conductor.common.run.WorkflowSummary;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.dao.IndexDAO;
import com.netflix.conductor.mongo.search.models.TaskExecutionLogs;
import com.netflix.conductor.mongo.search.models.TaskIndex;
import com.netflix.conductor.mongo.search.models.WorkflowIndex;
import com.netflix.conductor.mongo.search.repository.TaskExecutionLogsRepository;
import com.netflix.conductor.mongo.search.repository.TaskIndexRepository;
import com.netflix.conductor.mongo.search.repository.WorkflowIndexRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;


//@Component
public class MongoIndexDAO /*extends MongoSearchBaseDAO*/ /*implements IndexDAO*/ {

//    private final WorkflowIndexRepository workflowIndexRepository;
//
//    private final TaskIndexRepository taskIndexRepository;
//
//    private final TaskExecutionLogsRepository taskExecutionLogsRepository;
//
//    public MongoIndexDAO(
//            ObjectMapper objectMapper,
//            @Qualifier("MongoSearchRetryTemplate") RetryTemplate retryTemplate,
//            WorkflowIndexRepository workflowIndexRepository,
//            TaskIndexRepository taskIndexRepository,
//            TaskExecutionLogsRepository taskExecutionLogsRepository
//            ) {
//        super(objectMapper, retryTemplate);
//        this.workflowIndexRepository = workflowIndexRepository;
//        this.taskIndexRepository = taskIndexRepository;
//        this.taskExecutionLogsRepository = taskExecutionLogsRepository;
//    }
//
//    @Override
//    public void setup() {
//
//    }
//
//    @Override
//    public void indexWorkflow(WorkflowSummary workflow) {
//        TemporalAccessor temporalAccessor = DateTimeFormatter.ISO_INSTANT.parse(workflow.getStartTime());
//        Date startTime = Date.from(Instant.from(temporalAccessor));
//
//        WorkflowIndex workflowIndex = new WorkflowIndex();
//        workflowIndex.setWorkflowId(workflow.getWorkflowId());
//        workflowIndex.setCorrelationId(workflow.getCorrelationId());
//        workflowIndex.setWorkflowType(workflow.getWorkflowType());
//        workflowIndex.setStartTime(startTime);
//        workflowIndex.setStatus(workflow.getStatus().toString());
//        workflowIndex.setJsonData(toJson(workflow));
//        workflowIndexRepository.save(workflowIndex);
//    }
//
//    @Override
//    public CompletableFuture<Void> asyncIndexWorkflow(WorkflowSummary workflow) {
//        logger.info("asyncIndexWorkflow is not supported for mongodb indexing");
//        return null;
//    }
//
//    @Override
//    public void indexTask(TaskSummary task) {
//        TemporalAccessor startTimeTemporalAccessor = DateTimeFormatter.ISO_INSTANT.parse(task.getStartTime());
//        TemporalAccessor updateTimeTemporalAccessor = DateTimeFormatter.ISO_INSTANT.parse(task.getUpdateTime());
//        Date startTime = Date.from(Instant.from(startTimeTemporalAccessor));
//        Date updateTime = Date.from(Instant.from(updateTimeTemporalAccessor));
//
//        TaskIndex taskIndex = new TaskIndex();
//        taskIndex.setTaskId(task.getTaskId());
//        taskIndex.setTaskType(task.getTaskType());
//        taskIndex.setTaskDefName(task.getTaskDefName());
//        taskIndex.setStatus(task.getStatus().toString());
//        taskIndex.setStartTime(startTime);
//        taskIndex.setUpdateTime(updateTime);
//        taskIndex.setWorkflowType(task.getWorkflowType());
//        taskIndex.setJsonData(toJson(task));
//        taskIndexRepository.save(taskIndex);
//    }
//
//    @Override
//    public CompletableFuture<Void> asyncIndexTask(TaskSummary task) {
//        logger.info("asyncIndexTask is not supported for mongodb indexing");
//        return null;
//    }
//
//    @Override
//    public SearchResult<String> searchWorkflows(String query, String freeText, int start, int count, List<String> sort) {
//        logger.info("searchWorkflows is not supported for mongodb indexing");
//        return null;
//    }
//
//    @Override
//    public SearchResult<WorkflowSummary> searchWorkflowSummary(String query, String freeText, int start, int count, List<String> sort) {
//        logger.info("searchWorkflowSummary is not supported for mongodb indexing");
//        return null;
//    }
//
//    @Override
//    public SearchResult<String> searchTasks(String query, String freeText, int start, int count, List<String> sort) {
//        logger.info("searchTasks is not supported for mongodb indexing");
//        return null;
//    }
//
//    @Override
//    public SearchResult<TaskSummary> searchTaskSummary(String query, String freeText, int start, int count, List<String> sort) {
//        logger.info("searchTaskSummary is not supported for mongodb indexing");
//        // TODO can be implemented
//        return null;
//    }
//
//    @Override
//    public void removeWorkflow(String workflowId) {
//        logger.info("removeWorkflow is not supported for mongodb indexing");
//    }
//
//    @Override
//    public CompletableFuture<Void> asyncRemoveWorkflow(String workflowId) {
//        logger.info("asyncRemoveWorkflow is not supported for mongodb indexing");
//        return null;
//    }
//
//    @Override
//    public void updateWorkflow(String workflowInstanceId, String[] keys, Object[] values) {
//        logger.info("updateWorkflow is not supported for mongodb indexing");
//    }
//
//    @Override
//    public CompletableFuture<Void> asyncUpdateWorkflow(String workflowInstanceId, String[] keys, Object[] values) {
//        logger.info("asyncRemoveWorkflow is not supported for mongodb indexing");
//        return CompletableFuture.completedFuture(null);
//    }
//
//    @Override
//    public void removeTask(String workflowId, String taskId) {
//        logger.info("removeTask is not supported for mongodb indexing");
//    }
//
//    @Override
//    public CompletableFuture<Void> asyncRemoveTask(String workflowId, String taskId) {
//        logger.info("asyncRemoveTask is not supported for mongodb indexing");
//        return CompletableFuture.completedFuture(null);
//    }
//
//    @Override
//    public void updateTask(String workflowId, String taskId, String[] keys, Object[] values) {
//        logger.info("updateTask is not supported for mongodb indexing");
//    }
//
//    @Override
//    public CompletableFuture<Void> asyncUpdateTask(String workflowId, String taskId, String[] keys, Object[] values) {
//        logger.info("asyncUpdateTask is not supported for mongodb indexing");
//        return CompletableFuture.completedFuture(null);
//    }
//
//    @Override
//    public String get(String workflowInstanceId, String key) {
//        logger.info("get is not supported for mongodb indexing");
//        return null;
//    }
//
//    @Override
//    public void addTaskExecutionLogs(List<TaskExecLog> logs) {
//        if(logs == null || logs.isEmpty()) {
//            return;
//        }
//        logs.forEach(log -> {
//            TaskExecutionLogs taskExecutionLogs = new TaskExecutionLogs();
//            taskExecutionLogs.setTaskId(log.getTaskId());
//            taskExecutionLogs.setLog(log.getLog());
//            taskExecutionLogs.setCreatedTime(Date.from(new Timestamp(log.getCreatedTime()).toInstant()));
//            taskExecutionLogsRepository.save(taskExecutionLogs);
//        });
//    }
//
//    @Override
//    public CompletableFuture<Void> asyncAddTaskExecutionLogs(List<TaskExecLog> logs) {
//        logger.info("asyncAddTaskExecutionLogs is not supported for mongodb indexing");
//        return CompletableFuture.completedFuture(null);
//    }
//
//    @Override
//    public List<TaskExecLog> getTaskExecutionLogs(String taskId) {
//        List<TaskExecutionLogs> taskExecutionLogs = taskExecutionLogsRepository.findAllByTaskId(taskId);
//        if(taskExecutionLogs == null || taskExecutionLogs.isEmpty()) {
//            return Collections.emptyList();
//        }
//        return taskExecutionLogs.stream().map(exeLog -> {
//            TaskExecLog log = new TaskExecLog();
//            log.setTaskId(taskId);
//            log.setLog(exeLog.getLog());
//            log.setCreatedTime(exeLog.getCreatedTime().getTime());
//            return log;
//        }).toList();
//    }
//
//    @Override
//    public void addEventExecution(EventExecution eventExecution) {
//        logger.info("addEventExecution is not supported for mongodb indexing");
//    }
//
//    @Override
//    public List<EventExecution> getEventExecutions(String event) {
//        logger.info("getEventExecutions is not supported for mongodb indexing");
//        return null;
//    }
//
//    @Override
//    public CompletableFuture<Void> asyncAddEventExecution(EventExecution eventExecution) {
//        logger.info("asyncAddEventExecution is not supported for mongodb indexing");
//        return CompletableFuture.completedFuture(null);
//    }
//
//    @Override
//    public void addMessage(String queue, Message msg) {
//        logger.info("addMessage is not supported for mongodb indexing");
//    }
//
//    @Override
//    public CompletableFuture<Void> asyncAddMessage(String queue, Message message) {
//        logger.info("asyncAddMessage is not supported for mongodb indexing");
//        return CompletableFuture.completedFuture(null);
//    }
//
//    @Override
//    public List<Message> getMessages(String queue) {
//        logger.info("getMessages is not supported for mongodb indexing");
//        return null;
//    }
//
//    @Override
//    public List<String> searchArchivableWorkflows(String indexName, long archiveTtlDays) {
//        logger.info("searchArchivableWorkflows is not supported for mongodb indexing");
//        return null;
//    }
//
//    @Override
//    public long getWorkflowCount(String query, String freeText) {
//        logger.info("getWorkflowCount is not supported for mongodb indexing");
//        return 0;
//    }
}
