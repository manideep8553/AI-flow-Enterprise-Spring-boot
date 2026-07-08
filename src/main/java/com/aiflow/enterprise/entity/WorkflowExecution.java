package com.aiflow.enterprise.entity;

import com.aiflow.enterprise.entity.embedded.ExecutionLogEntry;
import com.aiflow.enterprise.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "workflow_executions")
public class WorkflowExecution extends BaseEntity {

    @Indexed
    private String workflowId;

    @Indexed
    private String workflowName;

    private Integer workflowVersion;

    @Indexed
    private ExecutionStatus status;

    private String currentStepId;

    private Instant startedAt;

    private Instant completedAt;

    private Instant suspendedAt;

    private String triggeredBy;

    private String triggerType;

    private String errorMessage;

    private Integer retryCount;

    private Map<String, Object> context;

    private Map<String, Object> stepStates;

    private Map<String, Integer> retryTracker;

    private List<ExecutionLogEntry> executionLog;

    private Map<String, Object> inputParams;

    private Map<String, Object> outputResults;

    private Long totalDurationMs;
}
