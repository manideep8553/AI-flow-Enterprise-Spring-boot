package com.aiflow.enterprise.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "scheduler_locks")
public class DistributedLock {

    @Id
    private String id;

    @Indexed(unique = true)
    private String lockKey;

    private String acquiredBy;

    private Instant acquiredAt;

    @Indexed
    private Instant expiresAt;

    private int ttlSeconds;

    @Builder.Default
    private int version = 1;
}
