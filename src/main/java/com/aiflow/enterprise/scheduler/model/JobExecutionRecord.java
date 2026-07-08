package com.aiflow.enterprise.scheduler.model;

import com.aiflow.enterprise.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "job_execution_records")
@CompoundIndex(name = "job_name_status", def = "{'jobName': 1, 'status': 1}")
@CompoundIndex(name = "job_name_started", def = "{'jobName': 1, 'startedAt': -1}")
public class JobExecutionRecord extends BaseEntity {

    @Indexed
    private String jobName;

    private String jobGroup;

    private String description;

    private JobStatus status;

    @Indexed
    private Instant startedAt;

    private Instant completedAt;

    private Long durationMs;

    private String triggeredBy;

    private boolean success;

    private String errorMessage;

    private String stackTrace;

    private int retryCount;

    private int maxRetries;

    private String lockedBy;

    private String nodeId;

    private Map<String, Object> metadata;
}
