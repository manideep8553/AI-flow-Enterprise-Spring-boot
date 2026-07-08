package com.aiflow.enterprise.scheduler.job;

import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class ScheduledWorkflowJob extends AbstractScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(ScheduledWorkflowJob.class);

    private final MongoTemplate mongoTemplate;

    public ScheduledWorkflowJob(DistributedLockService distributedLockService,
                                 JobMonitorService jobMonitorService,
                                 MongoTemplate mongoTemplate) {
        super(distributedLockService, jobMonitorService);
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String getJobName() {
        return "scheduled-workflow-executor";
    }

    @Override
    public String getJobGroup() {
        return "workflow";
    }

    @Override
    public String getDescription() {
        return "Executes workflows that are due for scheduled or recurring execution";
    }

    @Override
    public int getLockTtlSeconds() {
        return 300;
    }

    @Override
    public int getMaxRetries() {
        return 2;
    }

    @Override
    @Scheduled(cron = "${app.scheduler.jobs.scheduled-workflow.cron:0 */5 * * * *}")
    public void run() {
        super.run();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void execute() {
        Instant now = Instant.now();

        Query query = Query.query(Criteria.where("status").is("PUBLISHED")
                .and("metadata.scheduleEnabled").is(true)
                .orOperator(
                        Criteria.where("metadata.nextScheduledRunAt").lt(now),
                        Criteria.where("metadata.nextScheduledRunAt").is(null)
                ));

        List<Map> dueWorkflows = mongoTemplate.find(query, Map.class, "workflows");
        log.info("Found {} workflows due for execution", dueWorkflows.size());

        for (Map<String, Object> workflow : dueWorkflows) {
            try {
                String workflowId = workflow.get("_id") != null ? workflow.get("_id").toString() : null;
                if (workflowId == null) continue;

                Map<String, Object> execution = new java.util.HashMap<>();
                execution.put("workflowId", workflowId);
                execution.put("workflowName", workflow.get("name"));
                execution.put("status", "PENDING");
                execution.put("triggeredBy", "SCHEDULER");
                execution.put("startedAt", now);

                mongoTemplate.save(execution, "workflow_executions");
                log.info("Queued workflow execution: workflowId={}", workflowId);

            } catch (Exception e) {
                log.error("Failed to queue workflow {}: {}", workflow.get("_id"), e.getMessage());
            }
        }
    }
}
