package com.netflix.conductor.mongo.db.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.netflix.conductor.common.metadata.events.EventExecution;
import com.netflix.conductor.common.metadata.tasks.PollData;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.core.exception.NonTransientException;
import com.netflix.conductor.dao.ConcurrentExecutionLimitDAO;
import com.netflix.conductor.dao.ExecutionDAO;
import com.netflix.conductor.dao.PollDataDAO;
import com.netflix.conductor.dao.RateLimitingDAO;
import com.netflix.conductor.metrics.Monitors;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;
import com.netflix.conductor.mongo.db.models.*;
import com.netflix.conductor.mongo.db.repository.*;
;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MongoExecutionDAO extends MongoDBBaseDAO implements ExecutionDAO, RateLimitingDAO, PollDataDAO, ConcurrentExecutionLimitDAO {

    private final TaskInProgressRepository taskInProgressRepository;

    private final TaskRepository taskRepository;

    private final TaskScheduledRepository taskScheduledRepository;

    private final WorkflowToTaskRepository workflowToTaskRepository;

    private final WorkflowRepository workflowRepository;

    private final WorkflowDefToWorkflowRepository workflowDefToWorkflowRepository;

    private final WorkflowPendingRepository workflowPendingRepository;

    private final EventExecutionDocumentRepository eventExecutionDocumentRepository;

    private final PollDataDocumentRepository pollDataDocumentRepository;

    public MongoExecutionDAO(
            ObjectMapper objectMapper,
            @Qualifier("MongoDBRetryTemplate") RetryTemplate retryTemplate,
            TaskInProgressRepository taskInProgressRepository,
            TaskRepository taskRepository,
            TaskScheduledRepository taskScheduledRepository,
            WorkflowToTaskRepository workflowToTaskRepository,
            WorkflowRepository workflowRepository,
            WorkflowDefToWorkflowRepository workflowDefToWorkflowRepository,
            WorkflowPendingRepository workflowPendingRepository,
            EventExecutionDocumentRepository eventExecutionDocumentRepository,
            PollDataDocumentRepository pollDataDocumentRepository) {
        super(objectMapper, retryTemplate);
        this.taskInProgressRepository = taskInProgressRepository;
        this.taskRepository = taskRepository;
        this.taskScheduledRepository = taskScheduledRepository;
        this.workflowToTaskRepository = workflowToTaskRepository;
        this.workflowRepository = workflowRepository;
        this.workflowDefToWorkflowRepository = workflowDefToWorkflowRepository;
        this.workflowPendingRepository = workflowPendingRepository;
        this.eventExecutionDocumentRepository = eventExecutionDocumentRepository;
        this.pollDataDocumentRepository = pollDataDocumentRepository;
    }

    @Override
    public List<TaskModel> getPendingTasksByWorkflow(String taskName, String workflowId) {
        List<TaskInProgress> taskInProgresses = taskInProgressRepository
                .getAllByTaskDefNameAndWorkflowId(taskName, workflowId);
        return convertTaskInProgressToTaskModel(taskInProgresses);
    }

    @Override
    public List<TaskModel> getTasks(String taskType, String startKey, int count) {
        List<TaskModel> pendingTasks = getPendingTasksForTaskType(taskType);
        List<TaskModel> tasks = new ArrayList<>(count);
        boolean startKeyFound = startKey == null;
        int found = 0;
        for (TaskModel pendingTask : pendingTasks) {
            if (!startKeyFound) {
                if (pendingTask.getTaskId().equals(startKey)) {
                    startKeyFound = true;
                    // noinspection ConstantConditions
                    if (startKey != null) {
                        continue;
                    }
                }
            }
            if (startKeyFound && found < count) {
                tasks.add(pendingTask);
                found++;
            }
        }
        return tasks;
    }

    @Override
    public List<TaskModel> createTasks(List<TaskModel> taskModels) {
        List<TaskModel> created = Lists.newArrayListWithCapacity(taskModels.size());
        for(TaskModel taskModel: taskModels) {
            try {
                validate(taskModel);
            }catch (NullPointerException e) {
                throw new NonTransientException(e.getMessage());
            }
            taskModel.setScheduledTime(System.currentTimeMillis());
            final String taskKey = taskKey(taskModel);
            boolean scheduledTaskAdded = addScheduledTask(taskModel, taskKey);
            if (!scheduledTaskAdded) {
                logger.trace(
                        "Task already scheduled, skipping the run "
                                + taskModel.getTaskId()
                                + ", ref="
                                + taskModel.getReferenceTaskName()
                                + ", key="
                                + taskKey);
                continue;
            }
            createOrUpdateTaskModel(taskModel);
            addWorkflowToTaskMapping(taskModel);
            addTaskInProgress(taskModel);
            updateTask(taskModel);
            created.add(taskModel);
        }

        return created;
    }

    @Override
    public void updateTask(TaskModel taskModel) {
        Optional<TaskDef> taskDefinition = taskModel.getTaskDefinition();
        if (taskDefinition.isPresent() && taskDefinition.get().concurrencyLimit() > 0) {
            boolean inProgress = taskModel.getStatus() != null && taskModel.getStatus().equals(TaskModel.Status.IN_PROGRESS);
            updateInProgressStatus(taskModel, inProgress);
        }
        insertOrUpdateTaskData(taskModel);
        if (taskModel.getStatus() != null && taskModel.getStatus().isTerminal()) {
            removeTaskInProgress(taskModel);
        }
        addWorkflowToTaskMapping(taskModel);
    }

    @Override
    public boolean removeTask(String taskId) {
        TaskModel taskModel = getTask(taskId);
        if (taskModel == null) {
            logger.warn("No such task found by id {}", taskId);
            return false;
        }
        final String taskKey = taskKey(taskModel);
        removeScheduledTask(taskModel, taskKey);
        removeWorkflowToTaskMapping(taskModel);
        removeTaskInProgress(taskModel);
        taskRepository.removeTaskByTaskId(taskModel.getTaskId());
        return true;
    }

    @Override
    public TaskModel getTask(String taskId) {
        TaskDocument taskDocument = taskRepository.getTaskByTaskId(taskId);
        if(taskDocument == null || taskDocument.getJsonData() == null) {
            return null;
        }
        return readValue(taskDocument.getJsonData(), TaskModel.class);
    }

    @Override
    public List<TaskModel> getTasks(List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            return Lists.newArrayList();
        }
        // TODO Make Retryable
        List<TaskDocument> taskDocuments = taskRepository.getTasksByTaskId(taskIds);
        if (taskDocuments.isEmpty()) {
            return Lists.newArrayList();
        }
        return taskDocuments.stream().map(taskDocument -> readValue(taskDocument.getJsonData(), TaskModel.class)).toList();
    }

    @Override
    public List<TaskModel> getPendingTasksForTaskType(String taskType) {
        List<TaskInProgress> taskInProgresses = taskInProgressRepository.getTaskInProgressesByTaskDefName(taskType);
        return convertTaskInProgressToTaskModel(taskInProgresses);
    }

    @Override
    public List<TaskModel> getTasksForWorkflow(String workflowId) {
        List<WorkflowToTask> workflowToTasks = workflowToTaskRepository.getWorkflowToTasksByWorkflowId(workflowId);
        if(workflowToTasks == null || workflowToTasks.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> taskIds = workflowToTasks.stream().map(WorkflowToTask::getTaskId).toList();
        return getTasks(taskIds);
    }

    @Override
//    @Transactional // FIXME
    public String createWorkflow(WorkflowModel workflow) {
        return createOrUpdateWorkflow(workflow, false);
    }

    @Override
//    @Transactional // FIXME
    public String updateWorkflow(WorkflowModel workflow) {
        return createOrUpdateWorkflow(workflow, true);
    }

    @Override
    public boolean removeWorkflow(String workflowId) {
        WorkflowModel workflowModel = getWorkflow(workflowId, true);
        if (workflowModel == null) {
            return false;
        }
        removeWorkflowDefToWorkflowMapping(workflowModel);
        removePendingWorkflow(workflowModel.getWorkflowName(), workflowId);
        boolean result = true;
        for(TaskModel taskModel : workflowModel.getTasks()) {
            if(!removeTask(taskModel.getTaskId())) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean removeWorkflowWithExpiry(String workflowId, int ttlSeconds) {
        throw new UnsupportedOperationException(
            "This method is not implemented in MongoExecutionDAO. Please use RedisDAO mode instead for using TTLs."
        );
    }

    @Override
    public void removeFromPendingWorkflow(String workflowType, String workflowId) {
        removePendingWorkflow(workflowType, workflowId);
    }

    @Override
    public WorkflowModel getWorkflow(String workflowId) {
        return getWorkflow(workflowId, true);
    }

    @Override
    public WorkflowModel getWorkflow(String workflowId, boolean includeTasks) {
        WorkflowModel workflowModel = readWorkflowFromDB(workflowId);
        if(workflowModel == null) {
            return null;
        }
        if(includeTasks) {
            List<TaskModel> tasks = new ArrayList<>(getTasksForWorkflow(workflowId));
            tasks.sort(Comparator.comparingInt(TaskModel::getSeq));
            workflowModel.setTasks(tasks);
        }
        return workflowModel;
    }

    @Override
    public List<String> getRunningWorkflowIds(String workflowName, int version) {
        Preconditions.checkNotNull(workflowName, "workflowName cannot be null");
        List<WorkflowPending> workflowPendings = workflowPendingRepository
                .getWorkflowPendingsByWorkflowType(workflowName);
        if(workflowPendings == null || workflowPendings.isEmpty()) {
            return Collections.emptyList();
        }
        return workflowPendings.stream().map(WorkflowPending::getWorkflowId).toList();
    }

    @Override
    public List<WorkflowModel> getPendingWorkflowsByType(String workflowName, int version) {
        Preconditions.checkNotNull(workflowName, "workflowName cannot be null");
        return getRunningWorkflowIds(workflowName, version)
                .stream()
                .map(this::getWorkflow)
                .filter(workflow -> workflow.getWorkflowVersion() == version)
                .toList();
    }

    @Override
    public long getPendingWorkflowCount(String workflowName) {
        Preconditions.checkNotNull(workflowName, "workflowName cannot be null");
        return workflowPendingRepository.countWorkflowPendingsByWorkflowType(workflowName);
    }

    @Override
    public long getInProgressTaskCount(String taskDefName) {
        Preconditions.checkNotNull(taskDefName, "taskDefName cannot be null");
        return taskInProgressRepository.countTaskInProgressByTaskDefNameAndInProgressStatus(taskDefName, true);
    }

    @Override
    public List<WorkflowModel> getWorkflowsByType(String workflowName, Long startTime, Long endTime) {
        Preconditions.checkNotNull(workflowName, "workflowName cannot be null");
        Preconditions.checkNotNull(startTime, "startTime cannot be null");
        Preconditions.checkNotNull(endTime, "endTime cannot be null");
        List<WorkflowDefToWorkflow> workflowDefToWorkflows =
                workflowDefToWorkflowRepository.getByWorkflowDefAndBetweenDate(workflowName, startTime, endTime);
        if(workflowDefToWorkflows == null || workflowDefToWorkflows.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> workflowIds = workflowDefToWorkflows.stream().map(WorkflowDefToWorkflow::getWorkflowId).toList();
        return workflowIds.stream().map(this::getWorkflow).toList();
    }

    @Override
    public List<WorkflowModel> getWorkflowsByCorrelationId(String workflowName, String correlationId, boolean includeTasks) {
//        if (includeTasks) {
//            throw new UnsupportedOperationException(
//                "includeTasks is not implemented in MongoExecutionDAO.getWorkflowsByCorrelationId"
//            );
//        }
        Preconditions.checkNotNull(correlationId, "correlationId cannot be null");
        List<WorkflowDefToWorkflow> workflowDefToWorkflows =
                workflowDefToWorkflowRepository.getByWorkflowDef(workflowName);
        if(workflowDefToWorkflows == null || workflowDefToWorkflows.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> workflowIds = workflowDefToWorkflows.stream().map(WorkflowDefToWorkflow::getWorkflowId).toList();
        List<WorkflowDocument> workflowDocuments = workflowRepository.getByWorkflowIdsAndCorrelationId(workflowIds,correlationId);
        if(workflowDocuments == null || workflowDocuments.isEmpty()) {
            return Collections.emptyList();
        }
        return workflowDocuments.stream().map(workflowDocument -> readValue(workflowDocument.getJsonData(), WorkflowModel.class)).toList();
    }

    @Override
    public boolean canSearchAcrossWorkflows() {
        return true;
    }

    @Override
    public boolean addEventExecution(EventExecution eventExecution) {
        EventExecutionDocument eventExecutionDocument = new EventExecutionDocument();
        eventExecutionDocument.setEventHandlerName(eventExecution.getName());
        eventExecutionDocument.setEventName(eventExecution.getEvent());
        eventExecutionDocument.setMessageId(eventExecution.getMessageId());
        eventExecutionDocument.setExecutionId(eventExecution.getId());
        eventExecutionDocument.setJsonData(toJson(eventExecution));
        eventExecutionDocumentRepository.save(eventExecutionDocument);
        return false;
    }

    @Override
    public void updateEventExecution(EventExecution eventExecution) {
        eventExecutionDocumentRepository.updateEventExecutionDocument(
            eventExecution.getName(),
            eventExecution.getEvent(),
            eventExecution.getMessageId(),
            eventExecution.getId(),
            toJson(eventExecution)
        );
    }

    @Override
    public void removeEventExecution(EventExecution eventExecution) {
        eventExecutionDocumentRepository.deleteEventExecutionDocument(
            eventExecution.getName(),
            eventExecution.getEvent(),
            eventExecution.getMessageId(),
            eventExecution.getId()
        );
    }

    @Override
    public void updateLastPollData(String taskDefName, String domain, String workerId) {
        Preconditions.checkNotNull(taskDefName, "taskDefName name cannot be null");
        PollData pollData = new PollData(taskDefName, domain, workerId, System.currentTimeMillis());
        String effectiveDomain = (domain == null) ? "DEFAULT" : domain;
        insertOrUpdatePollData(pollData, effectiveDomain);
    }

    @Override
    public PollData getPollData(String taskDefName, String domain) {
        Preconditions.checkNotNull(taskDefName, "taskDefName name cannot be null");
        String effectiveDomain = (domain == null) ? "DEFAULT" : domain;
        return readPollData(taskDefName, effectiveDomain);
    }

    @Override
    public List<PollData> getPollData(String taskDefName) {
        Preconditions.checkNotNull(taskDefName, "taskDefName name cannot be null");
        return readAllPollData(taskDefName);
    }

    @Override
    public boolean exceedsRateLimitPerFrequency(TaskModel task, TaskDef taskDef) {
        return false;
    }

    @Override
    public void addTaskToLimit(TaskModel task) {
        ConcurrentExecutionLimitDAO.super.addTaskToLimit(task);
    }

    @Override
    public void removeTaskFromLimit(TaskModel task) {
        ConcurrentExecutionLimitDAO.super.removeTaskFromLimit(task);
    }

    @Override
    public boolean exceedsLimit(TaskModel task) {
        Optional<TaskDef> taskDefinition = task.getTaskDefinition();
        if (taskDefinition.isEmpty()) {
            return false;
        }
        TaskDef taskDef = taskDefinition.get();
        int limit = taskDef.concurrencyLimit();
        if (limit <= 0) {
            return false;
        }
        long current = getInProgressTaskCount(task.getTaskDefName());
        if (current >= limit) {
            Monitors.recordTaskConcurrentExecutionLimited(task.getTaskDefName(), limit);
            return true;
        }
        logger.info(
                "Task execution count details TASK={} LIMIT={}, CURRENT={}",
                task.getTaskDefName(),
                limit,
                getInProgressTaskCount(task.getTaskDefName()));

        String taskId = task.getTaskId();

        List<String> tasksInProgressInOrderOfArrival =
                findAllTasksInProgressInOrderOfArrival(task, limit);

        boolean rateLimited = !tasksInProgressInOrderOfArrival.contains(taskId);

        if (rateLimited) {
            logger.info(
                    "Task execution count limited. TASK={} LIMIT={}, CURRENT={}",
                    task.getTaskDefName(),
                    limit,
                    getInProgressTaskCount(task.getTaskDefName()));
            Monitors.recordTaskConcurrentExecutionLimited(task.getTaskDefName(), limit);
        }

        return rateLimited;
    }

    private List<String> findAllTasksInProgressInOrderOfArrival(TaskModel task, int limit) {
        return taskInProgressRepository
                .getAllByTaskDefNameOrderByCreatedOn(task.getTaskDefName())
                .stream()
                .limit(limit)
                .map(TaskInProgress::getTaskId)
                .toList();
    }

    private List<TaskModel> convertTaskInProgressToTaskModel(List<TaskInProgress> taskInProgresses) {
        if(taskInProgresses == null || taskInProgresses.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> taskIds = taskInProgresses.stream().map(TaskInProgress::getTaskId).toList();
        List<TaskDocument> taskDocuments = taskRepository.getTasksByTaskId(taskIds);
        return taskDocuments.stream().map(taskDocument -> readValue(taskDocument.getJsonData(), TaskModel.class)).toList();
    }

    private void validate(TaskModel taskModel) {
        Preconditions.checkNotNull(taskModel, "task object cannot be null");
        Preconditions.checkNotNull(taskModel.getTaskId(), "Task id cannot be null");
        Preconditions.checkNotNull(taskModel.getWorkflowInstanceId(), "Workflow instance id cannot be null");
        Preconditions.checkNotNull(taskModel.getReferenceTaskName(), "Task reference name cannot be null");
    }

    private static String taskKey(TaskModel taskModel) {
        return taskModel.getReferenceTaskName() + "_" + taskModel.getRetryCount();
    }

    private boolean addScheduledTask(TaskModel taskModel, String taskKey) {
        boolean isTaskExists = taskScheduledRepository
                .existsTaskScheduledByWorkflowIdAndTaskKey(taskModel.getWorkflowInstanceId(), taskKey);
        if(isTaskExists) {
            return false;
        }
        TaskScheduled taskScheduled = new TaskScheduled();
        taskScheduled.setWorkflowId(taskModel.getWorkflowInstanceId());
        taskScheduled.setTaskKey(taskKey);
        taskScheduled.setTaskId(taskModel.getTaskId());
        taskScheduledRepository.insert(taskScheduled);
        return true;
    }

    private void createOrUpdateTaskModel(TaskModel taskModel) {
        if(taskRepository.existsTaskByTaskId(taskModel.getTaskId())) {
            taskRepository.updateTaskByTaskId(taskModel.getTaskId(), toJson(taskModel));
        } else {
            TaskDocument taskDocument = new TaskDocument();
            taskDocument.setTaskId(taskModel.getTaskId());
            taskDocument.setJsonData(toJson(taskModel));
            taskRepository.save(taskDocument);
        }
    }

    private void addWorkflowToTaskMapping(TaskModel taskModel) {
        if(workflowToTaskRepository.existsWorkflowToTaskByWorkflowIdAndTaskId(
                taskModel.getWorkflowInstanceId(),
                taskModel.getTaskId()
        )) {
            return;
        }
        WorkflowToTask workflowToTask = new WorkflowToTask();
        workflowToTask.setWorkflowId(taskModel.getWorkflowInstanceId());
        workflowToTask.setTaskId(taskModel.getTaskId());
        workflowToTaskRepository.save(workflowToTask);
    }

    private void addTaskInProgress(TaskModel taskModel) {
        if(taskInProgressRepository.existsTaskInProgressByTaskDefNameAndTaskId(taskModel.getTaskDefName(), taskModel.getTaskId())) {
            return;
        }
        TaskInProgress taskInProgress = new TaskInProgress();
        taskInProgress.setTaskDefName(taskModel.getTaskDefName());
        taskInProgress.setTaskId(taskModel.getTaskId());
        taskInProgress.setWorkflowId(taskModel.getWorkflowInstanceId());
        taskInProgress.setTaskType(taskModel.getTaskType());
        taskInProgressRepository.save(taskInProgress);
    }

    private void updateInProgressStatus(TaskModel taskModel, boolean inProgress) {
        taskInProgressRepository.updateTaskInProgressByTaskDefNameAndTaskId(
                taskModel.getTaskDefName(),
                taskModel.getTaskId(),
                inProgress
        );
    }

    private void insertOrUpdateTaskData(TaskModel taskModel) {
        if (taskRepository.existsTaskByTaskId(taskModel.getTaskId())) {
            taskRepository.updateTaskByTaskId(taskModel.getTaskId(), toJson(taskModel));
            return;
        }
        TaskDocument taskDocument = new TaskDocument();
        taskDocument.setTaskId(taskModel.getTaskId());
        taskDocument.setJsonData(toJson(taskModel));
        taskRepository.save(taskDocument);
    }

    private void removeTaskInProgress(TaskModel taskModel) {
        taskInProgressRepository.removeTaskInProgressByTaskDefNameAndTaskId(
                taskModel.getTaskDefName(),
                taskModel.getTaskId()
        );
    }

    private void removeScheduledTask(TaskModel taskModel, String taskKey) {
        taskScheduledRepository.removeTaskScheduledByWorkflowIdAndTaskKey(
                taskModel.getWorkflowInstanceId(),
                taskKey
        );
    }

    private void removeWorkflowToTaskMapping(TaskModel taskModel) {
        workflowToTaskRepository.removeWorkflowToTaskByWorkflowIdAndTaskId(
                taskModel.getWorkflowInstanceId(),
                taskModel.getTaskId()
        );
    }

    private void addWorkflow(WorkflowModel workflowModel) {
        WorkflowDocument workflowDocument = new WorkflowDocument();
        workflowDocument.setWorkflowId(workflowModel.getWorkflowId());
        workflowDocument.setCorrelationId(workflowModel.getCorrelationId());
        workflowDocument.setJsonData(toJson(workflowModel));
        workflowRepository.save(workflowDocument);
    }

    private void addWorkflowDefToWorkflowMapping(WorkflowModel workflowModel) {
        WorkflowDefToWorkflow workflowDefToWorkflow = new WorkflowDefToWorkflow();
        workflowDefToWorkflow.setWorkflowDef(workflowModel.getWorkflowName());
        workflowDefToWorkflow.setDate(workflowModel.getCreateTime());
        workflowDefToWorkflow.setWorkflowId(workflowModel.getWorkflowId());
        workflowDefToWorkflowRepository.save(workflowDefToWorkflow);
    }

    private void updateWorkflowToDB(WorkflowModel workflowModel) {
        workflowRepository.updateWorkflowByWorkflowId(
                workflowModel.getWorkflowId(),
                toJson(workflowModel)
        );
    }

    private void removePendingWorkflow(String workflowType, String workflowId) {
        workflowPendingRepository.deleteWorkflowPendingByWorkflowTypeAndWorkflowId(workflowType, workflowId);
    }

    private void addPendingWorkflow(String workflowType, String workflowId) {
        if(workflowPendingRepository.existsWorkflowPendingByWorkflowTypeAndWorkflowId(workflowType, workflowId)) {
            return;
        }
        WorkflowPending workflowPending = new WorkflowPending();
        workflowPending.setWorkflowType(workflowType);
        workflowPending.setWorkflowId(workflowId);
        workflowPendingRepository.save(workflowPending);
    }

    private String createOrUpdateWorkflow(WorkflowModel workflowModel, boolean isUpdate) {
        Preconditions.checkNotNull(workflowModel, "workflow object cannot be null");
        boolean terminal = workflowModel.getStatus().isTerminal();
        List<TaskModel> tasks = workflowModel.getTasks();
        workflowModel.setTasks(Lists.newLinkedList());
        // Transaction - start
        if (!isUpdate) {
            addWorkflow(workflowModel);
            addWorkflowDefToWorkflowMapping(workflowModel);
        } else {
            updateWorkflowToDB(workflowModel);
        }
        if (terminal) {
            removePendingWorkflow(workflowModel.getWorkflowName(), workflowModel.getWorkflowId());
        } else {
            addPendingWorkflow(workflowModel.getWorkflowName(), workflowModel.getWorkflowId());
        }
        // Transaction - stop
        workflowModel.setTasks(tasks);
        return workflowModel.getWorkflowId();
    }

    private void removeWorkflowDefToWorkflowMapping(WorkflowModel workflowModel) {
        workflowDefToWorkflowRepository.removeWorkflowDefToWorkflowByWorkflowDefAndDateStrAndWorkflowId(
                workflowModel.getWorkflowName(),
                workflowModel.getCreateTime(),
                workflowModel.getWorkflowId()
        );
    }

    private WorkflowModel readWorkflowFromDB(String workflowId) {
        WorkflowDocument workflowDocument = workflowRepository.getFirstByWorkflowId(workflowId);
        if (workflowDocument == null || workflowDocument.getJsonData() == null) {
            return null;
        }
        return readValue(workflowDocument.getJsonData(), WorkflowModel.class);
    }

    private void insertOrUpdatePollData(PollData pollData, String domain) {
        if(pollDataDocumentRepository.existsByQueueNameAndDomain(pollData.getQueueName(), domain)) {
            pollDataDocumentRepository.updateFirstByQueueNameAndDomain(
                    pollData.getQueueName(),
                    domain,
                    toJson(pollData)
            );
            return;
        }
        PollDataDocument pollDataDocument = new PollDataDocument();
        pollDataDocument.setQueueName(pollData.getQueueName());
        pollDataDocument.setDomain(domain);
        pollDataDocument.setJsonData(toJson(pollData));
        pollDataDocumentRepository.save(pollDataDocument);
    }

    private PollData readPollData(String taskDefName, String domain) {
        PollDataDocument pollDataDocument = pollDataDocumentRepository.getFirstByQueueNameAndDomain(taskDefName, domain);
        if(pollDataDocument == null) {
            return null;
        }
        return readValue(pollDataDocument.getJsonData(), PollData.class);
    }

    private List<PollData> readAllPollData(String taskDefName) {
        List<PollDataDocument> pollDataDocuments = pollDataDocumentRepository.getAllByQueueName(taskDefName);
        if(pollDataDocuments == null || pollDataDocuments.isEmpty()) {
            return Collections.emptyList();
        }
        return pollDataDocuments
                .stream()
                .map(pollDataDocument -> readValue(pollDataDocument.getJsonData(), PollData.class))
                .toList();
    }
}
