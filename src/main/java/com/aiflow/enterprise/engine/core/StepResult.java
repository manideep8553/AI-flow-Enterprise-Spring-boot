package com.aiflow.enterprise.engine.core;

import com.aiflow.enterprise.enums.ExecutionStatus;

import java.util.Collections;
import java.util.Map;

public class StepResult {

    private final ExecutionStatus status;
    private final Object output;
    private final Map<String, Object> data;
    private final String errorMessage;
    private final String nextStepId;

    private StepResult(ExecutionStatus status, Object output, Map<String, Object> data,
                       String errorMessage, String nextStepId) {
        this.status = status;
        this.output = output;
        this.data = data != null ? Collections.unmodifiableMap(data) : Map.of();
        this.errorMessage = errorMessage;
        this.nextStepId = nextStepId;
    }

    public static StepResult success(Object output) {
        return new StepResult(ExecutionStatus.COMPLETED, output, null, null, null);
    }

    public static StepResult success(Object output, Map<String, Object> data) {
        return new StepResult(ExecutionStatus.COMPLETED, output, data, null, null);
    }

    public static StepResult failure(String errorMessage) {
        return new StepResult(ExecutionStatus.FAILED, null, null, errorMessage, null);
    }

    public static StepResult failure(String errorMessage, String nextStepId) {
        return new StepResult(ExecutionStatus.FAILED, null, null, errorMessage, nextStepId);
    }

    public static StepResult skipped() {
        return new StepResult(ExecutionStatus.CANCELLED, null, null, "Skipped", null);
    }

    public static StepResult pendingApproval(Map<String, Object> data) {
        return new StepResult(ExecutionStatus.PENDING, null, data, null, null);
    }

    public ExecutionStatus getStatus() { return status; }
    public Object getOutput() { return output; }
    public Map<String, Object> getData() { return data; }
    public String getErrorMessage() { return errorMessage; }
    public String getNextStepId() { return nextStepId; }

    public boolean isSuccess() { return status == ExecutionStatus.COMPLETED; }
    public boolean isFailure() { return status == ExecutionStatus.FAILED; }
}
