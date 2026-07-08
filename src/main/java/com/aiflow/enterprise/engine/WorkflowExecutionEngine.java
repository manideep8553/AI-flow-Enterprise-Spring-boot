package com.aiflow.enterprise.engine;

import com.aiflow.enterprise.entity.Workflow;
import com.aiflow.enterprise.entity.WorkflowExecution;
import com.aiflow.enterprise.entity.embedded.ExecutionLogEntry;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.engine.core.StepProcessor;
import com.aiflow.enterprise.engine.core.StepResult;
import com.aiflow.enterprise.enums.ExecutionStatus;
import com.aiflow.enterprise.enums.StepType;
import com.aiflow.enterprise.repository.WorkflowExecutionRepository;
import com.aiflow.enterprise.repository.WorkflowRepository;
import com.aiflow.enterprise.websocket.ExecutionMonitorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class WorkflowExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExecutionEngine.class);

    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowRepository workflowRepository;
    private final ExecutionMonitorHandler wsHandler;
    private final ExecutionScheduler scheduler;
    private final ObjectMapper objectMapper;
    private final Map<StepType, StepProcessor> processors;

    public WorkflowExecutionEngine(WorkflowExecutionRepository executionRepository,
                                   WorkflowRepository workflowRepository,
                                   ExecutionMonitorHandler wsHandler,
                                   ExecutionScheduler scheduler,
                                   ObjectMapper objectMapper,
                                   List<StepProcessor> processorList) {
        this.executionRepository = executionRepository;
        this.workflowRepository = workflowRepository;
        this.wsHandler = wsHandler;
        this.scheduler = scheduler;
        this.objectMapper = objectMapper;
        this.processors = processorList.stream()
                .collect(Collectors.toMap(StepProcessor::getType, p -> p));
    }

    @Async("workflowExecutor")
    public void startExecution(String executionId) {
        log.info("Engine starting execution: {}", executionId);
        WorkflowExecution exec = executionRepository.findById(executionId).orElse(null);
        if (exec == null) {
            log.error("Execution not found: {}", executionId);
            return;
        }

        Workflow workflow = workflowRepository.findById(exec.getWorkflowId()).orElse(null);
        if (workflow == null) {
            failExecution(exec, "Associated workflow not found");
            return;
        }

        ExecutionContext ctx = ExecutionContext.from(exec);
        if (exec.getInputParams() != null) ctx.getVariables().putAll(exec.getInputParams());
        ctx.setVariable("executionId", executionId);
        ctx.setVariable("workflowName", workflow.getName());

        List<WorkflowStep> steps = workflow.getSteps();
        if (steps == null || steps.isEmpty()) {
            completeExecution(exec, ctx, "No steps to execute");
            return;
        }

        broadcast(executionId, "EXECUTION_STARTED", "Workflow execution started", null);
        processSteps(executionId, steps, ctx, exec);
    }

    @Async("workflowExecutor")
    public void resumeExecution(String executionId) {
        log.info("Engine resuming execution: {}", executionId);
        WorkflowExecution exec = executionRepository.findById(executionId).orElse(null);
        if (exec == null) {
            log.error("Execution not found for resume: {}", executionId);
            return;
        }
        Workflow workflow = workflowRepository.findById(exec.getWorkflowId()).orElse(null);
        if (workflow == null) {
            failExecution(exec, "Associated workflow not found");
            return;
        }

        ExecutionContext ctx = ExecutionContext.from(exec);
        exec.setStatus(ExecutionStatus.RUNNING);
        exec.setSuspendedAt(null);
        executionRepository.save(exec);

        broadcast(executionId, "EXECUTION_RESUMED", "Workflow execution resumed", null);
        processSteps(executionId, workflow.getSteps(), ctx, exec);
    }

    @Async("workflowExecutor")
    public void retryStep(String executionId, String stepId) {
        log.info("Engine retrying step: {} in execution: {}", stepId, executionId);
        WorkflowExecution exec = executionRepository.findById(executionId).orElse(null);
        if (exec == null) return;
        Workflow workflow = workflowRepository.findById(exec.getWorkflowId()).orElse(null);
        if (workflow == null) return;

        ExecutionContext ctx = ExecutionContext.from(exec);
        WorkflowStep failedStep = workflow.getSteps().stream()
                .filter(s -> s.getStepId().equals(stepId)).findFirst().orElse(null);
        if (failedStep == null) {
            log.error("Step {} not found in workflow {}", stepId, exec.getWorkflowId());
            return;
        }

        exec.setStatus(ExecutionStatus.RUNNING);
        executionRepository.save(exec);
        executeSingleStep(executionId, failedStep, ctx, exec, workflow.getSteps());
    }

    private void processSteps(String executionId, List<WorkflowStep> steps,
                               ExecutionContext ctx, WorkflowExecution exec) {
        try {
            Map<String, WorkflowStep> stepMap = steps.stream()
                    .collect(Collectors.toMap(WorkflowStep::getStepId, s -> s));

            if (ctx.getCurrentStepId() != null) {
                String resumeFrom = ctx.getCurrentStepId();
                WorkflowStep resumeStep = stepMap.get(resumeFrom);
                if (resumeStep != null) {
                    executeSingleStep(executionId, resumeStep, ctx, exec, steps);
                    return;
                }
            }

            List<WorkflowStep> sorted = topoSort(steps);
            processSequential(executionId, sorted, ctx, exec, stepMap);
        } catch (Exception e) {
            log.error("Unexpected error in execution {}: {}", executionId, e.getMessage(), e);
            failExecution(exec, "Unexpected error: " + e.getMessage());
        }
    }

    private void processSequential(String executionId, List<WorkflowStep> sorted,
                                    ExecutionContext ctx, WorkflowExecution exec,
                                    Map<String, WorkflowStep> stepMap) {
        Set<String> completedSteps = new HashSet<>(ctx.getStepResults().keySet());
        int index = 0;

        while (index < sorted.size()) {
            if (ctx.isSuspended() || ctx.isCancelled()) break;
            WorkflowStep step = sorted.get(index);

            if (completedSteps.contains(step.getStepId())) {
                index++;
                continue;
            }

            if (step.getDependsOn() != null && !step.getDependsOn().isEmpty()) {
                boolean depsMet = step.getDependsOn().stream()
                        .allMatch(depId -> completedSteps.contains(depId)
                                && !(ctx.getStepErrors().containsKey(depId)));
                if (!depsMet) {
                    log.warn("Dependencies not met for step: {}, skipping", step.getStepId());
                    index++;
                    continue;
                }
            }

            String branchFrom = evaluateBranchSelector(step, ctx);
            if (branchFrom != null && !completedSteps.contains(branchFrom)) {
                index++;
                continue;
            }

            StepResult result = executeSingleStep(executionId, step, ctx, exec, sorted);
            if (result == null) return;

            completedSteps.add(step.getStepId());
            ctx.setStepResult(step.getStepId(), result.getOutput());

            if (result.isSuccess()) {
                if (result.getNextStepId() != null) {
                    index = findStepIndex(sorted, result.getNextStepId());
                    if (index < 0) index = sorted.size();
                    continue;
                }
                index++;
            } else if (result.isFailure()) {
                if (result.getNextStepId() != null) {
                    index = findStepIndex(sorted, result.getNextStepId());
                    if (index < 0) {
                        failExecution(exec, result.getErrorMessage());
                        return;
                    }
                    continue;
                }
                failExecution(exec, result.getErrorMessage());
                return;
            } else {
                return;
            }
        }

        if (!ctx.isSuspended() && !ctx.isCancelled()) {
            completeExecution(exec, ctx, "All steps completed");
        }
    }

    private StepResult executeSingleStep(String executionId, WorkflowStep step,
                                          ExecutionContext ctx, WorkflowExecution exec,
                                          List<WorkflowStep> allSteps) {
        if (ctx.isCancelled()) return null;

        boolean isLoopIteration = step.getType() == StepType.LOOP
                && ctx.getLoopCount(step.getStepId()) > 0;

        ctx.setCurrentStepId(step.getStepId());
        ctx.setSuspended(false);
        saveSnapshot(exec, ctx);

        appendLog(exec, step, ExecutionStatus.RUNNING,
                "Executing step: " + step.getName());
        broadcast(executionId, "STEP_STARTED", "Executing: " + step.getName(), step.getStepId());

        Instant stepStart = Instant.now();

        try {
            StepProcessor processor = processors.get(step.getType());
            if (processor == null) {
                String msg = "No processor found for step type: " + step.getType();
                appendLog(exec, step, ExecutionStatus.FAILED, msg);
                broadcast(executionId, "STEP_FAILED", msg, step.getStepId());
                return StepResult.failure(msg);
            }

            if (step.getTimeoutSeconds() != null && step.getTimeoutSeconds() > 0) {
                scheduler.scheduleTimeout(executionId, step.getStepId(),
                        step.getTimeoutSeconds(), () -> handleTimeout(executionId, step.getStepId()));
            }

            StepResult result = processor.execute(step, ctx);

            long duration = java.time.Duration.between(stepStart, Instant.now()).toMillis();

            if (result.isSuccess()) {
                ctx.setStepResult(step.getStepId(), result.getOutput());
                if (result.getData() != null) {
                    result.getData().forEach(ctx::setVariable);
                }

                if (step.getType() == StepType.DELAY) {
                    Map<?, ?> outputMap = result.getOutput() instanceof Map ? (Map<?, ?>) result.getOutput() : Map.of();
                    Object secondsVal = outputMap.get("seconds");
                    long delaySeconds = secondsVal != null ? Long.parseLong(secondsVal.toString()) : 5;
                    scheduler.scheduleDelay(executionId, step.getStepId(), delaySeconds,
                            () -> handleDelayedResume(executionId, step.getStepId(), allSteps));
                    appendLog(exec, step, ExecutionStatus.RUNNING,
                            "Delayed for " + delaySeconds + "s");
                    saveSnapshot(exec, ctx);
                    return result;
                }

                if (step.getType() == StepType.LOOP) {
                    Map<?, ?> data = result.getOutput() instanceof Map ? (Map<?, ?>) result.getOutput() : Map.of();
                    Object continueVal = data.get("continue");
                    boolean shouldContinue = continueVal != null
                            ? Boolean.parseBoolean(continueVal.toString()) : false;
                    if (shouldContinue) {
                        saveSnapshot(exec, ctx);
                        appendLog(exec, step, ExecutionStatus.RUNNING,
                                "Loop iteration " + ctx.getLoopCount(step.getStepId()) + " completed, continuing");
                        broadcast(executionId, "LOOP_ITERATION",
                                "Iteration " + ctx.getLoopCount(step.getStepId()), step.getStepId());
                        return executeSingleStep(executionId, step, ctx, exec, allSteps);
                    }
                }

                appendLogWithDuration(exec, step, ExecutionStatus.COMPLETED,
                        "Step completed: " + step.getName(), duration);
                broadcast(executionId, "STEP_COMPLETED",
                        "Completed: " + step.getName(), step.getStepId());
                return result;

            } else if (result.isFailure()) {
                return handleStepFailure(executionId, step, ctx, exec,
                        result.getErrorMessage(), stepStart, allSteps);
            } else {
                if (result.getStatus() == ExecutionStatus.PENDING) {
                    appendLog(exec, step, ExecutionStatus.PENDING,
                            "Step pending: " + step.getName());
                    broadcast(executionId, "STEP_PENDING",
                            "Pending: " + step.getName(), step.getStepId());
                    saveSnapshot(exec, ctx);
                }
                return result;
            }

        } catch (Exception e) {
            log.error("Step execution error: {} - {}", step.getName(), e.getMessage(), e);
            return handleStepFailure(executionId, step, ctx, exec,
                    e.getMessage(), stepStart, allSteps);
        }
    }

    private StepResult handleStepFailure(String executionId, WorkflowStep step,
                                          ExecutionContext ctx, WorkflowExecution exec,
                                          String errorMessage, Instant stepStart,
                                          List<WorkflowStep> allSteps) {
        long duration = java.time.Duration.between(stepStart, Instant.now()).toMillis();
        ctx.setStepError(step.getStepId(), errorMessage);

        if (step.getRetryConfig() != null && step.getRetryConfig().getMaxAttempts() > 0) {
            int attempt = ctx.incrementRetry(step.getStepId());
            int maxAttempts = step.getRetryConfig().getMaxAttempts();

            if (attempt < maxAttempts) {
                long delay = (long) (step.getRetryConfig().getDelaySeconds()
                        * Math.pow(step.getRetryConfig().getBackoffMultiplier(), attempt - 1));
                appendLogWithDuration(exec, step, ExecutionStatus.RUNNING,
                        String.format("Retry %d/%d in %ds: %s", attempt, maxAttempts, delay, errorMessage),
                        duration);
                broadcast(executionId, "STEP_RETRYING",
                        String.format("Retry %d/%d", attempt, maxAttempts), step.getStepId());
                scheduler.scheduleRetry(executionId, step, delay,
                        () -> retryStep(executionId, step.getStepId()));
                return StepResult.failure(errorMessage);
            }
        }

        String errorStepId = step.getErrorStepId();
        if (errorStepId != null && allSteps != null) {
            appendLogWithDuration(exec, step, ExecutionStatus.FAILED,
                    "Step failed, routing to error handler: " + errorStepId, duration);
            broadcast(executionId, "STEP_FAILED", "Failed: " + step.getName(), step.getStepId());
            saveSnapshot(exec, ctx);
            return StepResult.failure(errorMessage, errorStepId);
        }

        appendLogWithDuration(exec, step, ExecutionStatus.FAILED,
                "Step failed: " + errorMessage, duration);
        broadcast(executionId, "STEP_FAILED", "Failed: " + errorMessage, step.getStepId());
        return StepResult.failure(errorMessage);
    }

    private void handleDelayedResume(String executionId, String stepId, List<WorkflowStep> allSteps) {
        WorkflowExecution exec = executionRepository.findById(executionId).orElse(null);
        if (exec == null || exec.getStatus() != ExecutionStatus.RUNNING) return;

        ExecutionContext ctx = ExecutionContext.from(exec);
        WorkflowStep step = allSteps.stream()
                .filter(s -> s.getStepId().equals(stepId)).findFirst().orElse(null);
        if (step == null) return;

        appendLog(exec, step, ExecutionStatus.COMPLETED, "Delay completed, resuming");
        broadcast(executionId, "STEP_COMPLETED", "Delay completed", stepId);

        int nextIdx = findStepIndex(allSteps, stepId) + 1;
        if (nextIdx < allSteps.size()) {
            Map<String, WorkflowStep> stepMap = allSteps.stream()
                    .collect(Collectors.toMap(WorkflowStep::getStepId, s -> s));
            processSequential(executionId, allSteps.subList(nextIdx, allSteps.size()),
                    ctx, exec, stepMap);
        } else {
            completeExecution(exec, ctx, "All steps completed after delay");
        }
    }

    private void handleTimeout(String executionId, String stepId) {
        WorkflowExecution exec = executionRepository.findById(executionId).orElse(null);
        if (exec == null) return;
        log.warn("Step {} timed out in execution {}", stepId, executionId);
        appendLog(exec, null, ExecutionStatus.TIMED_OUT,
                "Step " + stepId + " timed out");
        broadcast(executionId, "STEP_TIMED_OUT", "Step timed out: " + stepId, stepId);
    }

    private String evaluateBranchSelector(WorkflowStep step, ExecutionContext ctx) {
        if (step.getDependsOn() == null || step.getDependsOn().isEmpty()) return null;
        String selector = step.getCompletionCondition();
        if (selector == null || selector.isBlank()) return null;

        if (selector.startsWith("{{") && selector.endsWith("}}")) {
            String key = selector.substring(2, selector.length() - 2).trim();
            Object val = ctx.getStepResult(key);
            return val != null ? key : null;
        }
        return null;
    }

    private int findStepIndex(List<WorkflowStep> steps, String stepId) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getStepId().equals(stepId)) return i;
        }
        return -1;
    }

    private void completeExecution(WorkflowExecution exec, ExecutionContext ctx, String message) {
        exec.setStatus(ExecutionStatus.COMPLETED);
        exec.setCompletedAt(Instant.now());
        exec.setCurrentStepId(null);
        exec.setErrorMessage(null);
        ctx.snapshotTo(exec);
        appendLog(exec, null, ExecutionStatus.COMPLETED, message);
        exec.setTotalDurationMs(exec.getStartedAt() != null
                ? Instant.now().toEpochMilli() - exec.getStartedAt().toEpochMilli() : 0);
        executionRepository.save(exec);
        scheduler.cancelAllForExecution(exec.getId());
        broadcast(exec.getId(), "EXECUTION_COMPLETED",
                "Workflow completed in " + exec.getTotalDurationMs() + "ms", null);
        log.info("Execution {} completed", exec.getId());
    }

    private void failExecution(WorkflowExecution exec, String errorMessage) {
        exec.setStatus(ExecutionStatus.FAILED);
        exec.setCompletedAt(Instant.now());
        exec.setErrorMessage(errorMessage);
        exec.setCurrentStepId(null);
        if (exec.getExecutionLog() == null) exec.setExecutionLog(new ArrayList<>());
        exec.getExecutionLog().add(ExecutionLogEntry.builder()
                .stepId("system").stepName("Error")
                .status(ExecutionStatus.FAILED).message(errorMessage)
                .timestamp(Instant.now()).build());
        exec.setTotalDurationMs(exec.getStartedAt() != null
                ? Instant.now().toEpochMilli() - exec.getStartedAt().toEpochMilli() : 0);
        executionRepository.save(exec);
        scheduler.cancelAllForExecution(exec.getId());
        broadcast(exec.getId(), "EXECUTION_FAILED", errorMessage, null);
        log.error("Execution {} failed: {}", exec.getId(), errorMessage);
    }

    private void saveSnapshot(WorkflowExecution exec, ExecutionContext ctx) {
        ctx.snapshotTo(exec);
        if (exec.getExecutionLog() == null) exec.setExecutionLog(new ArrayList<>());
        exec.getExecutionLog().addAll(ctx.getLog());
        executionRepository.save(exec);
    }

    private void appendLog(WorkflowExecution exec, WorkflowStep step,
                            ExecutionStatus status, String message) {
        if (exec.getExecutionLog() == null) exec.setExecutionLog(new ArrayList<>());
        exec.getExecutionLog().add(ExecutionLogEntry.builder()
                .stepId(step != null ? step.getStepId() : "system")
                .stepName(step != null ? step.getName() : "System")
                .status(status).message(message).timestamp(Instant.now()).build());
    }

    private void appendLogWithDuration(WorkflowExecution exec, WorkflowStep step,
                                        ExecutionStatus status, String message, long durationMs) {
        if (exec.getExecutionLog() == null) exec.setExecutionLog(new ArrayList<>());
        exec.getExecutionLog().add(ExecutionLogEntry.builder()
                .stepId(step != null ? step.getStepId() : "system")
                .stepName(step != null ? step.getName() : "System")
                .status(status).message(message).timestamp(Instant.now())
                .durationMs(durationMs).build());
    }

    private void broadcast(String executionId, String event, String message, String stepId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", event);
            payload.put("message", message);
            payload.put("stepId", stepId);
            payload.put("timestamp", Instant.now().toString());
            wsHandler.sendExecutionUpdate(executionId, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.warn("Broadcast failed for {}: {}", executionId, e.getMessage());
        }
    }

    private List<WorkflowStep> topoSort(List<WorkflowStep> steps) {
        List<WorkflowStep> sorted = new ArrayList<>();
        Map<String, WorkflowStep> stepMap = steps.stream()
                .collect(Collectors.toMap(WorkflowStep::getStepId, s -> s));
        Map<String, Integer> inDegree = new HashMap<>();
        for (WorkflowStep s : steps) inDegree.putIfAbsent(s.getStepId(), 0);
        for (WorkflowStep s : steps) {
            if (s.getDependsOn() != null) {
                for (String dep : s.getDependsOn()) {
                    inDegree.merge(s.getStepId(), 1, Integer::sum);
                }
            }
        }
        Queue<String> queue = new LinkedList<>();
        inDegree.entrySet().stream()
                .filter(e -> e.getValue() == 0).map(Map.Entry::getKey).forEach(queue::add);

        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            WorkflowStep node = stepMap.get(nodeId);
            if (node != null) sorted.add(node);
            for (WorkflowStep s : steps) {
                if (s.getDependsOn() != null && s.getDependsOn().contains(nodeId)) {
                    inDegree.merge(s.getStepId(), -1, Integer::sum);
                    if (inDegree.get(s.getStepId()) == 0) queue.add(s.getStepId());
                }
            }
        }
        return sorted;
    }
}
