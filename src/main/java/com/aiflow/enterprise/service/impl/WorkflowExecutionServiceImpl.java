package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.response.ExecutionResponse;
import com.aiflow.enterprise.entity.Task;
import com.aiflow.enterprise.entity.Workflow;
import com.aiflow.enterprise.entity.WorkflowExecution;
import com.aiflow.enterprise.entity.embedded.ExecutionLogEntry;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.engine.WorkflowExecutionEngine;
import com.aiflow.enterprise.enums.ExecutionStatus;
import com.aiflow.enterprise.enums.TaskPriority;
import com.aiflow.enterprise.enums.TaskStatus;
import com.aiflow.enterprise.exception.BadRequestException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.mapper.ExecutionMapper;
import com.aiflow.enterprise.repository.TaskRepository;
import com.aiflow.enterprise.repository.WorkflowExecutionRepository;
import com.aiflow.enterprise.service.WorkflowExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class WorkflowExecutionServiceImpl implements WorkflowExecutionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionServiceImpl.class);

    private final WorkflowExecutionRepository executionRepository;
    private final TaskRepository taskRepository;
    private final ExecutionMapper executionMapper;
    private final WorkflowExecutionEngine engine;

    public WorkflowExecutionServiceImpl(WorkflowExecutionRepository executionRepository,
                                        TaskRepository taskRepository,
                                        ExecutionMapper executionMapper,
                                        WorkflowExecutionEngine engine) {
        this.executionRepository = executionRepository;
        this.taskRepository = taskRepository;
        this.executionMapper = executionMapper;
        this.engine = engine;
    }

    @Override
    @Transactional(readOnly = true)
    public ExecutionResponse getExecutionById(String id) {
        WorkflowExecution execution = findExecutionOrThrow(id);
        return executionMapper.toResponse(execution);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExecutionResponse> getAllExecutions(int page, int size, String workflowId,
                                                     String status, String triggeredBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WorkflowExecution> executionPage;

        if (workflowId != null && status != null) {
            ExecutionStatus execStatus = ExecutionStatus.valueOf(status.toUpperCase());
            executionPage = executionRepository.findByWorkflowIdAndStatus(workflowId, execStatus, pageable);
        } else if (workflowId != null) {
            executionPage = executionRepository.findByWorkflowId(workflowId, pageable);
        } else if (status != null) {
            ExecutionStatus execStatus = ExecutionStatus.valueOf(status.toUpperCase());
            executionPage = executionRepository.findByStatus(execStatus, pageable);
        } else if (triggeredBy != null) {
            executionPage = executionRepository.findByTriggeredBy(triggeredBy, pageable);
        } else {
            executionPage = executionRepository.findAll(pageable);
        }

        return executionPage.map(executionMapper::toResponse);
    }

    @Override
    public String createExecution(Workflow workflow, String triggeredBy, Map<String, Object> inputParams) {
        List<ExecutionLogEntry> initialLog = new ArrayList<>();
        initialLog.add(ExecutionLogEntry.builder()
                .stepId("init").stepName("Initialization")
                .status(ExecutionStatus.RUNNING)
                .message("Workflow execution initialized")
                .timestamp(Instant.now()).build());

        WorkflowExecution execution = WorkflowExecution.builder()
                .workflowId(workflow.getId())
                .workflowName(workflow.getName())
                .workflowVersion(workflow.getVersion())
                .status(ExecutionStatus.PENDING)
                .startedAt(Instant.now())
                .triggeredBy(triggeredBy != null ? triggeredBy : "system")
                .triggerType("manual")
                .inputParams(inputParams)
                .context(inputParams != null ? new HashMap<>(inputParams) : new HashMap<>())
                .stepStates(new HashMap<>())
                .retryTracker(new HashMap<>())
                .executionLog(initialLog)
                .build();

        WorkflowExecution saved = executionRepository.save(execution);
        log.info("Execution created: {}", saved.getId());
        return saved.getId();
    }

    @Override
    public ExecutionResponse startExecution(String id) {
        WorkflowExecution exec = findExecutionOrThrow(id);
        if (exec.getStatus() != ExecutionStatus.PENDING) {
            throw new BadRequestException("Execution can only be started from PENDING status");
        }
        exec.setStatus(ExecutionStatus.RUNNING);
        executionRepository.save(exec);
        engine.startExecution(id);
        log.info("Execution started: {}", id);
        return executionMapper.toResponse(exec);
    }

    @Override
    public ExecutionResponse cancelExecution(String id) {
        WorkflowExecution execution = findExecutionOrThrow(id);
        if (execution.getStatus() == ExecutionStatus.COMPLETED
                || execution.getStatus() == ExecutionStatus.CANCELLED
                || execution.getStatus() == ExecutionStatus.FAILED) {
            throw new BadRequestException("Cannot cancel execution in status: " + execution.getStatus());
        }
        execution.setStatus(ExecutionStatus.CANCELLED);
        execution.setCompletedAt(Instant.now());
        addSystemLog(execution, "Execution cancelled by user");

        List<Task> pendingTasks = taskRepository.findByExecutionId(id);
        for (Task task : pendingTasks) {
            if (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.IN_PROGRESS) {
                task.setStatus(TaskStatus.SKIPPED);
                taskRepository.save(task);
            }
        }
        executionRepository.save(execution);
        log.info("Execution cancelled: {}", id);
        return executionMapper.toResponse(execution);
    }

    @Override
    public ExecutionResponse suspendExecution(String id) {
        WorkflowExecution execution = findExecutionOrThrow(id);
        if (execution.getStatus() != ExecutionStatus.RUNNING) {
            throw new BadRequestException("Only running executions can be suspended");
        }
        execution.setStatus(ExecutionStatus.SUSPENDED);
        execution.setSuspendedAt(Instant.now());
        addSystemLog(execution, "Execution suspended");
        executionRepository.save(execution);
        log.info("Execution suspended: {}", id);
        return executionMapper.toResponse(execution);
    }

    @Override
    public ExecutionResponse resumeExecution(String id) {
        WorkflowExecution execution = findExecutionOrThrow(id);
        if (execution.getStatus() != ExecutionStatus.SUSPENDED) {
            throw new BadRequestException("Only suspended executions can be resumed");
        }
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setSuspendedAt(null);
        addSystemLog(execution, "Execution resumed");
        executionRepository.save(execution);
        engine.resumeExecution(id);
        log.info("Execution resumed: {}", id);
        return executionMapper.toResponse(execution);
    }

    @Override
    public ExecutionResponse retryStep(String executionId, String stepId) {
        WorkflowExecution execution = findExecutionOrThrow(executionId);
        if (execution.getStatus() != ExecutionStatus.FAILED) {
            throw new BadRequestException("Can only retry steps on failed executions");
        }
        engine.retryStep(executionId, stepId);
        log.info("Retrying step {} in execution {}", stepId, executionId);
        return executionMapper.toResponse(execution);
    }

    @Override
    public ExecutionResponse retryExecution(String id) {
        WorkflowExecution execution = findExecutionOrThrow(id);
        if (execution.getStatus() != ExecutionStatus.FAILED) {
            throw new BadRequestException("Can only retry failed executions");
        }
        execution.setStatus(ExecutionStatus.RUNNING);
        execution.setErrorMessage(null);
        addSystemLog(execution, "Execution retry initiated");
        executionRepository.save(execution);
        engine.startExecution(id);
        log.info("Execution retry: {}", id);
        return executionMapper.toResponse(execution);
    }

    private void addSystemLog(WorkflowExecution exec, String message) {
        if (exec.getExecutionLog() == null) exec.setExecutionLog(new ArrayList<>());
        exec.getExecutionLog().add(ExecutionLogEntry.builder()
                .stepId("system").stepName("System")
                .status(exec.getStatus()).message(message)
                .timestamp(Instant.now()).build());
    }

    private WorkflowExecution findExecutionOrThrow(String id) {
        return executionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowExecution", "id", id));
    }
}
