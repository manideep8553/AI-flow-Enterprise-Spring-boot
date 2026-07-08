package com.aiflow.enterprise.scheduler.job;

import com.aiflow.enterprise.scheduler.service.DistributedLockService;
import com.aiflow.enterprise.scheduler.service.JobMonitorService;
import com.aiflow.enterprise.service.EmailService;
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
public class ReminderEmailJob extends AbstractScheduledJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderEmailJob.class);

    private final MongoTemplate mongoTemplate;
    private final EmailService emailService;

    public ReminderEmailJob(DistributedLockService distributedLockService,
                             JobMonitorService jobMonitorService,
                             MongoTemplate mongoTemplate,
                             EmailService emailService) {
        super(distributedLockService, jobMonitorService);
        this.mongoTemplate = mongoTemplate;
        this.emailService = emailService;
    }

    @Override
    public String getJobName() {
        return "reminder-email-sender";
    }

    @Override
    public String getJobGroup() {
        return "notification";
    }

    @Override
    public String getDescription() {
        return "Sends reminder emails for pending requests and overdue tasks";
    }

    @Override
    public int getLockTtlSeconds() {
        return 180;
    }

    @Override
    public int getMaxRetries() {
        return 2;
    }

    @Override
    @Scheduled(cron = "${app.scheduler.jobs.reminder-email.cron:0 0 */6 * * *}")
    public void run() {
        super.run();
    }

    @Override
    protected void execute() {
        Instant now = Instant.now();
        int reminded = 0;

        Query pendingQuery = Query.query(Criteria.where("status").in("SUBMITTED", "PENDING_APPROVAL", "IN_PROGRESS"));
        List<Map> pendingRequests = mongoTemplate.find(pendingQuery, Map.class, "requests");

        for (Map<String, Object> request : pendingRequests) {
            try {
                String email = extractEmail(request);
                if (email == null) continue;

                String title = (String) request.getOrDefault("title", "Request");
                String id = request.get("_id") != null ? request.get("_id").toString() : "";

                emailService.sendSimpleEmail(email,
                        "Reminder: " + title + " requires attention",
                        "Your request (" + id + ") is still pending. Please take action.");
                reminded++;
            } catch (Exception e) {
                log.warn("Failed to send reminder for request {}: {}", request.get("_id"), e.getMessage());
            }
        }

        Query overdueQuery = Query.query(Criteria.where("status").in("PENDING", "IN_PROGRESS")
                .and("dueDate").lt(now));
        List<Map> overdueTasks = mongoTemplate.find(overdueQuery, Map.class, "tasks");

        for (Map<String, Object> task : overdueTasks) {
            try {
                String email = extractTaskAssigneeEmail(task);
                if (email == null) continue;

                String name = (String) task.getOrDefault("name", "Task");
                String id = task.get("_id") != null ? task.get("_id").toString() : "";

                emailService.sendSimpleEmail(email,
                        "Overdue: " + name,
                        "Task (" + id + ") is overdue. Please complete it as soon as possible.");
                reminded++;
            } catch (Exception e) {
                log.warn("Failed to send overdue notice for task {}: {}", task.get("_id"), e.getMessage());
            }
        }

        log.info("Reminder emails sent: {} notifications", reminded);
    }

    private String extractEmail(Map<String, Object> request) {
        String submittedBy = (String) request.get("submittedBy");
        if (submittedBy == null) return null;
        Query userQuery = Query.query(Criteria.where("username").is(submittedBy));
        Map user = mongoTemplate.findOne(userQuery, Map.class, "users");
        return user != null ? (String) user.get("email") : null;
    }

    private String extractTaskAssigneeEmail(Map<String, Object> task) {
        String assignee = (String) task.get("assignee");
        if (assignee == null) return null;
        Query userQuery = Query.query(Criteria.where("username").is(assignee));
        Map user = mongoTemplate.findOne(userQuery, Map.class, "users");
        return user != null ? (String) user.get("email") : null;
    }
}
