package com.aiflow.enterprise.dto.response;

import com.aiflow.enterprise.dto.response.log.ExecutionLogEntryResponse;
import com.aiflow.enterprise.enums.ExecutionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionResponse {

    private String id;
    private String workflowId;
    private String workflowName;
    private Integer workflowVersion;
    private ExecutionStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private String triggeredBy;
    private String triggerType;
    private String errorMessage;
    private List<ExecutionLogEntryResponse> executionLog;
    private Map<String, Object> inputParams;
    private Map<String, Object> outputResults;
    private Long totalDurationMs;
    private Instant createdAt;
    private Instant updatedAt;
}
