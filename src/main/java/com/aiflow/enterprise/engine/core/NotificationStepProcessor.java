package com.aiflow.enterprise.engine.core;

import com.aiflow.enterprise.engine.ExecutionContext;
import com.aiflow.enterprise.entity.User;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.enums.StepType;
import com.aiflow.enterprise.notification.dto.SendNotificationRequest;
import com.aiflow.enterprise.notification.enums.NotificationChannel;
import com.aiflow.enterprise.notification.enums.NotificationPriority;
import com.aiflow.enterprise.notification.enums.NotificationType;
import com.aiflow.enterprise.notification.service.NotificationService;
import com.aiflow.enterprise.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class NotificationStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationStepProcessor.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationStepProcessor(NotificationService notificationService,
                                     UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @Override
    public StepType getType() {
        return StepType.NOTIFICATION;
    }

    @Override
    public StepResult execute(WorkflowStep step, ExecutionContext ctx) {
        Map<String, Object> config = step.getConfig() != null ? step.getConfig() : Map.of();

        try {
            return executeNotification(step, ctx, config);
        } catch (Exception e) {
            log.error("Notification step execution error for step {}: {}", step.getStepId(), e.getMessage(), e);
            return StepResult.failure("Notification step failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private StepResult executeNotification(WorkflowStep step, ExecutionContext ctx,
                                           Map<String, Object> config) {
        String subject = resolveTemplate(getConfig(config, "subject", ""), ctx);
        String body = resolveTemplate(getConfig(config, "body", ""), ctx);
        String htmlBody = resolveTemplate(getConfig(config, "htmlBody", ""), ctx);

        String templateId = resolveTemplate(getConfig(config, "templateId", ""), ctx);
        String templateName = resolveTemplate(getConfig(config, "templateName", ""), ctx);

        String senderId = resolveTemplate(getConfig(config, "senderId", "system"), ctx);
        String senderName = resolveTemplate(getConfig(config, "senderName", "AIFlow System"), ctx);

        String priorityStr = getConfig(config, "priority", "MEDIUM").toUpperCase();
        String typeStr = getConfig(config, "type", "CUSTOM").toUpperCase();
        String locale = getConfig(config, "locale", null);
        String scheduleAtStr = getConfig(config, "scheduleAt", null);
        String expiresAtStr = getConfig(config, "expiresAt", null);
        int maxRetries = parseIntConfig(config, "maxRetries", 3);

        NotificationPriority priority = parsePriority(priorityStr);
        NotificationType type = parseType(typeStr);

        List<NotificationChannel> channels = resolveChannels(config);

        Map<String, Object> contextData = resolveContextData(config, ctx);

        Object recipientsRaw = config.get("recipients");
        List<String> recipientIds = new ArrayList<>();
        if (recipientsRaw instanceof List) {
            for (Object r : (List<Object>) recipientsRaw) {
                String resolved = resolveTemplate(r != null ? r.toString() : "", ctx);
                if (!resolved.isBlank()) recipientIds.add(resolved);
            }
        } else {
            String singleId = resolveTemplate(getConfig(config, "recipientId", ""), ctx);
            if (!singleId.isBlank()) recipientIds.add(singleId);
        }

        if (recipientIds.isEmpty()) {
            return StepResult.failure("No recipients configured for notification step");
        }

        Instant scheduleAt = scheduleAtStr != null && !scheduleAtStr.isBlank()
                ? Instant.parse(scheduleAtStr) : null;
        Instant expiresAt = expiresAtStr != null && !expiresAtStr.isBlank()
                ? Instant.parse(expiresAtStr) : null;

        List<Map<String, Object>> deliveryResults = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        StringBuilder errorSummary = new StringBuilder();

        for (String recipientId : recipientIds) {
            try {
                Map<String, Object> result = sendToRecipient(
                        recipientId, subject, body, htmlBody,
                        templateId, templateName, contextData,
                        channels, priority, type, locale,
                        scheduleAt, expiresAt, maxRetries,
                        senderId, senderName, step, ctx
                );
                deliveryResults.add(result);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    successCount++;
                } else {
                    failureCount++;
                    String err = (String) result.get("error");
                    if (err != null) {
                        if (errorSummary.length() > 0) errorSummary.append("; ");
                        errorSummary.append("[").append(recipientId).append("] ").append(err);
                    }
                }
            } catch (Exception e) {
                failureCount++;
                String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                if (errorSummary.length() > 0) errorSummary.append("; ");
                errorSummary.append("[").append(recipientId).append("] ").append(errMsg);

                Map<String, Object> failedResult = new HashMap<>();
                failedResult.put("recipientId", recipientId);
                failedResult.put("success", false);
                failedResult.put("error", errMsg);
                deliveryResults.add(failedResult);
            }
        }

        Map<String, Object> output = new HashMap<>();
        output.put("totalRecipients", recipientIds.size());
        output.put("successCount", successCount);
        output.put("failureCount", failureCount);
        output.put("channel", channels.stream().map(Enum::name).collect(Collectors.joining(",")));
        output.put("subject", subject);
        output.put("results", deliveryResults);

        Map<String, Object> data = new HashMap<>();
        data.put("notification_delivery_count", successCount);
        data.put("notification_failure_count", failureCount);
        data.put("notification_channels", channels.stream().map(Enum::name).toList());

        if (failureCount > 0) {
            String errMsg = errorSummary.length() > 0
                    ? errorSummary.toString()
                    : "Failed to send notifications to " + failureCount + " recipient(s)";
            if (successCount > 0) {
                output.put("error", errMsg);
                return StepResult.success(output, data);
            }
            return StepResult.failure(errMsg);
        }

        return StepResult.success(output, data);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> sendToRecipient(String recipientId, String subject, String body,
                                                  String htmlBody, String templateId, String templateName,
                                                  Map<String, Object> contextData,
                                                  List<NotificationChannel> channels,
                                                  NotificationPriority priority, NotificationType type,
                                                  String locale, Instant scheduleAt, Instant expiresAt,
                                                  int maxRetries, String senderId, String senderName,
                                                  WorkflowStep step, ExecutionContext ctx) {
        Map<String, Object> result = new HashMap<>();
        result.put("recipientId", recipientId);

        User user = userRepository.findById(recipientId).orElse(null);
        if (user == null) {
            log.warn("Recipient user not found: {}", recipientId);
            result.put("success", false);
            result.put("error", "User not found: " + recipientId);
            return result;
        }

        String resolvedSubject = resolveTemplate(subject, ctx);
        String resolvedBody = resolveTemplate(body, ctx);
        String resolvedHtmlBody = resolveTemplate(htmlBody != null ? htmlBody : "", ctx);

        resolvedSubject = resolveUserContext(resolvedSubject, user);
        resolvedBody = resolveUserContext(resolvedBody, user);
        resolvedHtmlBody = resolveUserContext(resolvedHtmlBody, user);

        String resolvedTemplateId = templateId != null && !templateId.isBlank() ? templateId : null;
        String resolvedTemplateName = templateName != null && !templateName.isBlank() ? templateName : null;

        SendNotificationRequest request = SendNotificationRequest.builder()
                .recipientId(recipientId)
                .recipientEmail(user.getEmail())
                .recipientName(user.getFirstName() + " " + user.getLastName())
                .senderId(senderId)
                .senderName(senderName)
                .type(type)
                .priority(priority)
                .channels(channels)
                .subject(resolvedSubject.isBlank() ? null : resolvedSubject)
                .body(resolvedBody.isBlank() ? null : resolvedBody)
                .htmlBody(resolvedHtmlBody.isBlank() ? null : resolvedHtmlBody)
                .templateId(resolvedTemplateId)
                .templateName(resolvedTemplateName)
                .contextData(contextData)
                .locale(locale)
                .scheduleAt(scheduleAt)
                .expiresAt(expiresAt)
                .maxRetries(maxRetries)
                .workflowId(ctx.getWorkflowId())
                .workflowExecutionId(ctx.getExecutionId())
                .correlationId(UUID.randomUUID().toString())
                .build();

        var response = notificationService.sendAndPublish(request);

        boolean success = response != null
                && response.getStatus() != null
                && !response.getStatus().name().contains("FAILED");

        result.put("success", success);
        result.put("notificationId", response != null ? response.getId() : null);
        result.put("status", response != null ? response.getStatus().name() : "FAILED");
        if (response != null && response.getLastError() != null) {
            result.put("error", response.getLastError());
        }
        result.put("channel", channels.stream().map(Enum::name).collect(Collectors.joining(",")));

        log.info("Notification sent to user {} ({}) via {}: id={} status={}",
                recipientId, user.getEmail(), channels, response != null ? response.getId() : "N/A",
                response != null ? response.getStatus() : "FAILED");

        return result;
    }

    @SuppressWarnings("unchecked")
    private List<NotificationChannel> resolveChannels(Map<String, Object> config) {
        Object channelsRaw = config.get("channels");
        if (channelsRaw instanceof List) {
            List<String> channelNames = new ArrayList<>();
            for (Object ch : (List<Object>) channelsRaw) {
                if (ch != null) channelNames.add(ch.toString().toUpperCase());
            }
            if (!channelNames.isEmpty()) {
                return channelNames.stream()
                        .map(this::parseChannel)
                        .filter(c -> c != null)
                        .collect(Collectors.toList());
            }
        }
        return List.of(NotificationChannel.IN_APP, NotificationChannel.EMAIL);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveContextData(Map<String, Object> config, ExecutionContext ctx) {
        Object contextRaw = config.get("context");
        Map<String, Object> resolved = new HashMap<>();

        resolved.put("executionId", ctx.getExecutionId());
        resolved.put("workflowId", ctx.getWorkflowId());
        resolved.put("workflowName", ctx.getWorkflowName());
        resolved.putAll(ctx.getVariables());

        if (contextRaw instanceof Map) {
            Map<String, Object> rawContext = (Map<String, Object>) contextRaw;
            for (Map.Entry<String, Object> entry : rawContext.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                String resolvedValue = resolveTemplate(value, ctx);
                resolved.put(entry.getKey(), resolvedValue);
            }
        }

        return resolved;
    }

    private String resolveUserContext(String template, User user) {
        if (template == null || template.isBlank()) return template;
        String result = template;
        result = result.replace("{{user.id}}", user.getId() != null ? user.getId() : "");
        result = result.replace("{{user.email}}", user.getEmail() != null ? user.getEmail() : "");
        result = result.replace("{{user.firstName}}", user.getFirstName() != null ? user.getFirstName() : "");
        result = result.replace("{{user.lastName}}", user.getLastName() != null ? user.getLastName() : "");
        result = result.replace("{{user.fullName}}",
                (user.getFirstName() != null ? user.getFirstName() : "")
                        + " " + (user.getLastName() != null ? user.getLastName() : ""));
        result = result.replace("{{user.username}}", user.getUsername() != null ? user.getUsername() : "");
        return result;
    }

    private NotificationChannel parseChannel(String name) {
        try {
            return NotificationChannel.valueOf(name);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown notification channel: {}", name);
            return null;
        }
    }

    private NotificationPriority parsePriority(String value) {
        try {
            return NotificationPriority.valueOf(value);
        } catch (IllegalArgumentException e) {
            return NotificationPriority.MEDIUM;
        }
    }

    private NotificationType parseType(String value) {
        try {
            return NotificationType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return NotificationType.CUSTOM;
        }
    }

    private int parseIntConfig(Map<String, Object> config, String key, int defaultValue) {
        Object val = config != null ? config.get(key) : null;
        if (val instanceof Number) return ((Number) val).intValue();
        if (val != null) {
            try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return defaultValue; }
        }
        return defaultValue;
    }
}
