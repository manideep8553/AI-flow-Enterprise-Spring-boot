package com.aiflow.enterprise.notification.service;

import com.aiflow.enterprise.entity.User;
import com.aiflow.enterprise.entity.Workflow;
import com.aiflow.enterprise.entity.WorkflowExecution;
import com.aiflow.enterprise.notification.dto.SendNotificationRequest;
import com.aiflow.enterprise.notification.enums.NotificationChannel;
import com.aiflow.enterprise.notification.enums.NotificationPriority;
import com.aiflow.enterprise.notification.enums.NotificationType;
import com.aiflow.enterprise.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowNotificationBridge {

    private static final Logger log = LoggerFactory.getLogger(WorkflowNotificationBridge.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public WorkflowNotificationBridge(NotificationService notificationService,
                                      UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    public void onWorkflowStarted(Workflow workflow, WorkflowExecution execution, String triggeredBy) {
        sendNotification(
                triggeredBy,
                NotificationType.WORKFLOW_STARTED,
                NotificationPriority.LOW,
                "Workflow Started: " + workflow.getName(),
                "Workflow '" + workflow.getName() + "' (v" + workflow.getVersion()
                        + ") has been started. Execution ID: " + execution.getId(),
                null,
                workflow.getId(),
                execution.getId(),
                buildContext("workflowName", workflow.getName(), "executionId", execution.getId())
        );
    }

    public void onWorkflowCompleted(Workflow workflow, WorkflowExecution execution, String triggeredBy) {
        sendNotification(
                triggeredBy,
                NotificationType.WORKFLOW_COMPLETED,
                NotificationPriority.MEDIUM,
                "Workflow Completed: " + workflow.getName(),
                "Workflow '" + workflow.getName() + "' completed successfully. "
                        + "Total duration: " + execution.getTotalDurationMs() + "ms",
                null,
                workflow.getId(),
                execution.getId(),
                buildContext("workflowName", workflow.getName(), "status", "COMPLETED",
                        "durationMs", execution.getTotalDurationMs())
        );
    }

    public void onWorkflowFailed(Workflow workflow, WorkflowExecution execution,
                                  String errorMessage, String triggeredBy) {
        sendNotification(
                triggeredBy,
                NotificationType.WORKFLOW_FAILED,
                NotificationPriority.HIGH,
                "Workflow Failed: " + workflow.getName(),
                "Workflow '" + workflow.getName() + "' failed. Error: " + errorMessage,
                null,
                workflow.getId(),
                execution.getId(),
                buildContext("workflowName", workflow.getName(), "error", errorMessage)
        );
    }

    public void onApprovalRequested(String requestId, String requestTitle, String approverId,
                                     String submitterName, double amount) {
        sendNotification(
                approverId,
                NotificationType.APPROVAL_REQUESTED,
                NotificationPriority.HIGH,
                "Approval Required: " + requestTitle,
                "Approval request from " + submitterName
                        + (amount > 0 ? " for $" + String.format("%.2f", amount) : "")
                        + ". Request: " + requestTitle,
                requestId,
                null,
                null,
                buildContext("requestId", requestId, "title", requestTitle,
                        "amount", amount, "submittedBy", submitterName)
        );
    }

    public void onApprovalCompleted(String requestId, String requestTitle, String userId,
                                     boolean approved, String reviewerName) {
        NotificationType type = approved ? NotificationType.REQUEST_APPROVED
                : NotificationType.REQUEST_REJECTED;
        String action = approved ? "approved" : "rejected";

        sendNotification(
                userId,
                type,
                NotificationPriority.MEDIUM,
                "Request " + (approved ? "Approved" : "Rejected") + ": " + requestTitle,
                "Your request '" + requestTitle + "' was " + action + " by " + reviewerName + ".",
                requestId,
                null,
                null,
                buildContext("requestId", requestId, "title", requestTitle,
                        "approved", approved, "reviewedBy", reviewerName)
        );
    }

    public void onTaskAssigned(String taskId, String taskTitle, String assigneeId,
                                String assignedByName, String workflowName) {
        sendNotification(
                assigneeId,
                NotificationType.TASK_ASSIGNED,
                NotificationPriority.MEDIUM,
                "Task Assigned: " + taskTitle,
                "You have been assigned task '" + taskTitle + "'"
                        + (workflowName != null ? " in workflow '" + workflowName + "'" : "")
                        + " by " + assignedByName + ".",
                null,
                null,
                null,
                buildContext("taskId", taskId, "title", taskTitle,
                        "assignedBy", assignedByName, "workflowName", workflowName)
        );
    }

    public void onTaskOverdue(String taskId, String taskTitle, String assigneeId,
                               String workflowName) {
        sendNotification(
                assigneeId,
                NotificationType.TASK_OVERDUE,
                NotificationPriority.URGENT,
                "Task Overdue: " + taskTitle,
                "Task '" + taskTitle + "'"
                        + (workflowName != null ? " in workflow '" + workflowName + "'" : "")
                        + " is now overdue. Please complete it as soon as possible.",
                null,
                null,
                null,
                buildContext("taskId", taskId, "title", taskTitle, "workflowName", workflowName)
        );
    }

    public void onFraudAlert(String userId, String requestId, double riskScore,
                              String riskLevel, List<String> flaggedCategories) {
        NotificationPriority priority = "CRITICAL".equals(riskLevel)
                ? NotificationPriority.URGENT : NotificationPriority.HIGH;
        NotificationType type = "CRITICAL".equals(riskLevel)
                ? NotificationType.FRAUD_ALERT_CRITICAL : NotificationType.FRAUD_ALERT_HIGH;

        sendNotification(
                userId,
                type,
                priority,
                "Fraud Alert: " + riskLevel + " Risk",
                "Fraud detection flagged a " + riskLevel.toLowerCase()
                        + " risk transaction (score: " + String.format("%.2f", riskScore) + "). "
                        + "Flagged categories: " + String.join(", ", flaggedCategories),
                requestId,
                null,
                null,
                buildContext("riskScore", riskScore, "riskLevel", riskLevel,
                        "flaggedCategories", flaggedCategories)
        );
    }

    private void sendNotification(String recipientId, NotificationType type,
                                   NotificationPriority priority, String subject,
                                   String body, String requestId,
                                   String workflowId, String executionId,
                                   Map<String, Object> context) {
        try {
            if (recipientId == null) return;

            User user = userRepository.findById(recipientId).orElse(null);
            if (user == null) return;

            SendNotificationRequest request = SendNotificationRequest.builder()
                    .recipientId(recipientId)
                    .recipientEmail(user.getEmail())
                    .recipientName(user.getFirstName() + " " + user.getLastName())
                    .type(type)
                    .priority(priority)
                    .channels(List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL))
                    .subject(subject)
                    .body(body)
                    .contextData(context)
                    .requestId(requestId)
                    .workflowId(workflowId)
                    .workflowExecutionId(executionId)
                    .correlationId(UUID.randomUUID().toString())
                    .build();

            notificationService.sendAndPublish(request);
            log.debug("Notification sent: type={} userId={} subject={}", type, recipientId, subject);

        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildContext(Object... kvPairs) {
        Map<String, Object> ctx = new HashMap<>();
        for (int i = 0; i < kvPairs.length; i += 2) {
            if (i + 1 < kvPairs.length) {
                ctx.put(String.valueOf(kvPairs[i]), kvPairs[i + 1]);
            }
        }
        return ctx;
    }
}
