package com.aiflow.enterprise.entity.embedded;

import com.aiflow.enterprise.enums.ExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionLogEntry {
    private String stepId;
    private String stepName;
    private ExecutionStatus status;
    private String message;
    private String errorDetail;
    private Instant timestamp;
    private Long durationMs;
}
