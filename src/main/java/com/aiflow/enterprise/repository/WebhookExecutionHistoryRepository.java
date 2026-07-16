package com.aiflow.enterprise.repository;

import com.aiflow.enterprise.entity.WebhookExecutionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface WebhookExecutionHistoryRepository extends MongoRepository<WebhookExecutionHistory, String> {

    Page<WebhookExecutionHistory> findByExecutionId(String executionId, Pageable pageable);

    Page<WebhookExecutionHistory> findByUrl(String url, Pageable pageable);

    List<WebhookExecutionHistory> findByExecutionIdAndStepIdOrderByStartedAtDesc(String executionId, String stepId);

    Page<WebhookExecutionHistory> findBySuccessAndStartedAtAfter(boolean success, Instant after, Pageable pageable);

    long countByUrlAndStartedAtAfter(String url, Instant after);

    long countByUrlAndSuccessAndStartedAtAfter(String url, boolean success, Instant after);
}
