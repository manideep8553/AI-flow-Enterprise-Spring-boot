package com.aiflow.enterprise.dto.response.log;

import com.aiflow.enterprise.enums.ExecutionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionLogEntryResponse {

    private String stepId;
    private String stepName;
    private ExecutionStatus status;
    private String message;
    private String errorDetail;
    private Instant timestamp;
    private Long durationMs;
}
