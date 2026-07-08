package com.aiflow.enterprise.engine;

import com.aiflow.enterprise.entity.WorkflowExecution;
import com.aiflow.enterprise.entity.embedded.ExecutionLogEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExecutionContext {

    private final String executionId;
    private final String workflowId;
    private final String workflowName;
    private final Map<String, Object> variables;
    private final Map<String, Object> stepResults;
    private final Map<String, Object> stepErrors;
    private final Map<String, Integer> retryAttempts;
    private final Map<String, Integer> loopIterations;
    private final List<ExecutionLogEntry> log;
    private String currentStepId;
    private volatile boolean suspended;
    private volatile boolean cancelled;

    public ExecutionContext(String executionId, String workflowId, String workflowName) {
        this.executionId = executionId;
        this.workflowId = workflowId;
        this.workflowName = workflowName;
        this.variables = new HashMap<>();
        this.stepResults = new HashMap<>();
        this.stepErrors = new HashMap<>();
        this.retryAttempts = new HashMap<>();
        this.loopIterations = new HashMap<>();
        this.log = new ArrayList<>();
    }

    public static ExecutionContext from(WorkflowExecution exec) {
        ExecutionContext ctx = new ExecutionContext(
                exec.getId(), exec.getWorkflowId(), exec.getWorkflowName());
        if (exec.getContext() != null) ctx.variables.putAll(exec.getContext());
        if (exec.getStepStates() != null) {
            for (Map.Entry<String, Object> e : exec.getStepStates().entrySet()) {
                ctx.stepResults.put(e.getKey(), e.getValue());
            }
        }
        if (exec.getRetryTracker() != null) {
            for (Map.Entry<String, Integer> e : exec.getRetryTracker().entrySet()) {
                ctx.retryAttempts.put(e.getKey(), e.getValue());
            }
        }
        if (exec.getExecutionLog() != null) ctx.log.addAll(exec.getExecutionLog());
        ctx.currentStepId = exec.getCurrentStepId();
        return ctx;
    }

    public void snapshotTo(WorkflowExecution exec) {
        exec.setContext(new HashMap<>(variables));
        exec.setStepStates(new HashMap<>(stepResults));
        exec.setRetryTracker(new HashMap<>(retryAttempts));
        exec.setCurrentStepId(currentStepId);
    }

    public String getExecutionId() { return executionId; }
    public String getWorkflowId() { return workflowId; }
    public String getWorkflowName() { return workflowName; }
    public Map<String, Object> getVariables() { return variables; }
    public Map<String, Object> getStepResults() { return stepResults; }
    public Map<String, Object> getStepErrors() { return stepErrors; }
    public Map<String, Integer> getRetryAttempts() { return retryAttempts; }
    public Map<String, Integer> getLoopIterations() { return loopIterations; }
    public List<ExecutionLogEntry> getLog() { return log; }
    public String getCurrentStepId() { return currentStepId; }
    public void setCurrentStepId(String currentStepId) { this.currentStepId = currentStepId; }
    public boolean isSuspended() { return suspended; }
    public void setSuspended(boolean suspended) { this.suspended = suspended; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    public void setVariable(String key, Object value) { variables.put(key, value); }
    public Object getVariable(String key) { return variables.get(key); }
    @SuppressWarnings("unchecked")
    public <T> T getVariable(String key, Class<T> type) {
        Object val = variables.get(key);
        return val != null && type.isInstance(val) ? (T) val : null;
    }

    public void setStepResult(String stepId, Object result) { stepResults.put(stepId, result); }
    public Object getStepResult(String stepId) { return stepResults.get(stepId); }

    public void setStepError(String stepId, Object error) { stepErrors.put(stepId, error); }
    public Object getStepError(String stepId) { return stepErrors.get(stepId); }

    public int incrementRetry(String stepId) {
        int count = retryAttempts.getOrDefault(stepId, 0) + 1;
        retryAttempts.put(stepId, count);
        return count;
    }

    public int getRetryCount(String stepId) { return retryAttempts.getOrDefault(stepId, 0); }

    public int incrementLoop(String stepId) {
        int count = loopIterations.getOrDefault(stepId, 0) + 1;
        loopIterations.put(stepId, count);
        return count;
    }

    public int getLoopCount(String stepId) { return loopIterations.getOrDefault(stepId, 0); }

    public void addLog(ExecutionLogEntry entry) { log.add(entry); }
}
