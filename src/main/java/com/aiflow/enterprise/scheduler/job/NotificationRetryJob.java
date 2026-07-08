package com.aiflow.enterprise.scheduler.job;

import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import com.aiflow.enterprise.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Component
public class NotificationRetryJob extends AbstractScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetryJob.class);

    private static final int MAX_RETRIES = 5;
    private static final long[] BACKOFF_MINUTES = {1, 5, 15, 30, 60};

    private final MongoTemplate mongoTemplate;
    private final EmailService emailService;

    public NotificationRetryJob(DistributedLockService distributedLockService,
                                 JobMonitorService jobMonitorService,
                                 MongoTemplate mongoTemplate,
                                 EmailService emailService) {
        super(distributedLockService, jobMonitorService);
        this.mongoTemplate = mongoTemplate;
        this.emailService = emailService;
    }

    @Override
    public String getJobName() {
        return "notification-retry";
    }

    @Override
    public String getJobGroup() {
        return "notification";
    }

    @Override
    public String getDescription() {
        return "Retries failed email and notification deliveries with exponential backoff";
    }

    @Override
    public int getLockTtlSeconds() {
        return 180;
    }

    @Override
    public int getMaxRetries() {
        return 1;
    }

    @Override
    @Scheduled(cron = "${app.scheduler.jobs.notification-retry.cron:0 */5 * * * *}")
    public void run() {
        super.run();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void execute() {
        Instant now = Instant.now();
        int retried = 0;
        int permanentlyFailed = 0;

        Query query = Query.query(Criteria.where("status").is("FAILED")
                .and("retryCount").lt(MAX_RETRIES));

        List<Map> failedNotifications = mongoTemplate.find(query, Map.class, "notification_queue");

        for (Map<String, Object> notification : failedNotifications) {
            try {
                String id = (String) notification.get("_id");
                Object retryCountObj = notification.get("retryCount");
                int retryCount = retryCountObj instanceof Number ? ((Number) retryCountObj).intValue() : 0;
                Object lastAttemptObj = notification.get("lastAttemptAt");
                Instant lastAttempt = lastAttemptObj instanceof Instant ? (Instant) lastAttemptObj : null;

                long backoffMinutes = BACKOFF_MINUTES[Math.min(retryCount, BACKOFF_MINUTES.length - 1)];
                if (lastAttempt != null && lastAttempt.plus(backoffMinutes, ChronoUnit.MINUTES).isAfter(now)) {
                    continue;
                }

                String type = (String) notification.get("type");
                String recipient = (String) notification.get("recipient");
                String subject = (String) notification.get("subject");
                String content = (String) notification.get("content");

                boolean success = false;
                try {
                    if ("EMAIL".equals(type)) {
                        emailService.sendSimpleEmail(recipient, subject, content);
                        success = true;
                    }
                } catch (Exception e) {
                    log.warn("Retry {}/{} failed for notification {}: {}",
                            retryCount + 1, MAX_RETRIES, id, e.getMessage());
                }

                if (success) {
                    mongoTemplate.remove(Query.query(Criteria.where("_id").is(id)), "notification_queue");
                    retried++;
                } else {
                    int newRetryCount = retryCount + 1;
                    if (newRetryCount >= MAX_RETRIES) {
                        mongoTemplate.remove(Query.query(Criteria.where("_id").is(id)), "notification_queue");
                        log.warn("Notification {} permanently failed after {} retries", id, MAX_RETRIES);
                        permanentlyFailed++;
                    } else {
                        mongoTemplate.findAndModify(
                                Query.query(Criteria.where("_id").is(id)),
                                new Update()
                                        .set("retryCount", newRetryCount)
                                        .set("lastAttemptAt", now),
                                Map.class, "notification_queue"
                        );
                    }
                }

            } catch (Exception e) {
                log.warn("Failed to process notification retry: {}", e.getMessage());
            }
        }

        log.info("Notification retry complete: {} retried, {} permanently failed", retried, permanentlyFailed);
    }
}
