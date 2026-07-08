package com.aiflow.enterprise.scheduler.repository;

import com.aiflow.enterprise.scheduler.model.JobExecutionRecord;
import com.aiflow.enterprise.scheduler.model.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface JobExecutionRecordRepository extends MongoRepository<JobExecutionRecord, String> {

    Page<JobExecutionRecord> findByJobNameOrderByStartedAtDesc(String jobName, Pageable pageable);

    Page<JobExecutionRecord> findByJobNameAndStatusOrderByStartedAtDesc(String jobName, JobStatus status, Pageable pageable);

    Page<JobExecutionRecord> findByStatusOrderByStartedAtDesc(JobStatus status, Pageable pageable);

    List<JobExecutionRecord> findByStatusAndStartedAtBefore(JobStatus status, Instant startedAt);

    List<JobExecutionRecord> findByJobNameAndStatus(String jobName, JobStatus status);

    long countByJobNameAndStatus(String jobName, JobStatus status);

    long countByJobNameAndSuccessAndStartedAtAfter(String jobName, boolean success, Instant after);

    long countByJobNameAndStartedAtAfter(String jobName, Instant after);

    @Aggregation(pipeline = {
            "{'$group': {'_id': '$jobName', 'lastRun': {'$max': '$startedAt'}, 'totalRuns': {'$sum': 1}, 'failures': {'$sum': {'$cond': [{'$eq': ['$success', false]}, 1, 0]}}}}",
            "{'$project': {'jobName': '$_id', 'lastRun': 1, 'totalRuns': 1, 'failures': 1, 'lastStatus': 1}}",
            "{'$sort': {'lastRun': -1}}"
    })
    List<JobSummary> getJobSummaries();

    interface JobSummary {
        String getJobName();
        Instant getLastRun();
        long getTotalRuns();
        long getFailures();
    }
}
