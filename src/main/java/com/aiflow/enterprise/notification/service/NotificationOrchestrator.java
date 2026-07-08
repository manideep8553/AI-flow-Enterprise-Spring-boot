package com.aiflow.enterprise.notification.service;

import com.aiflow.enterprise.notification.dto.SendNotificationRequest;
import com.aiflow.enterprise.notification.entity.Notification;
import com.aiflow.enterprise.notification.entity.NotificationPreference;
import com.aiflow.enterprise.notification.entity.NotificationTemplate;
import com.aiflow.enterprise.notification.entity.embedded.DeliveryAttempt;
import com.aiflow.enterprise.notification.enums.NotificationChannel;
import com.aiflow.enterprise.notification.enums.NotificationPriority;
import com.aiflow.enterprise.notification.enums.NotificationStatus;
import com.aiflow.enterprise.notification.enums.NotificationType;
import com.aiflow.enterprise.notification.repository.NotificationPreferenceRepository;
import com.aiflow.enterprise.notification.repository.NotificationRepository;
import com.aiflow.enterprise.notification.service.NotificationTemplateService.RenderedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(NotificationOrchestrator.class);

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationTemplateService templateService;
    private final NotificationDeliveryService deliveryService;
    private final NotificationEventPublisher eventPublisher;

    public NotificationOrchestrator(NotificationRepository notificationRepository,
                                    NotificationPreferenceRepository preferenceRepository,
                                    NotificationTemplateService templateService,
                                    NotificationDeliveryService deliveryService,
                                    NotificationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.templateService = templateService;
        this.deliveryService = deliveryService;
        this.eventPublisher = eventPublisher;
    }

    public Notification processNotificationRequest(SendNotificationRequest request) {
        try {
            NotificationTemplate template = null;
            if (request.getTemplateId() != null || request.getTemplateName() != null) {
                template = templateService.findTemplate(
                        request.getTemplateId() != null ? request.getTemplateId() : request.getTemplateName());
            }

            if (template == null && request.getType() != null) {
                template = templateService.findTemplateByType(request.getType());
            }

            List<NotificationChannel> channels = resolveChannels(request, template);

            NotificationPreference preference = preferenceRepository
                    .findByUserId(request.getRecipientId()).orElse(null);

            if (preference != null) {
                channels = filterByPreference(channels, request.getType(), preference);
            }

            if (channels.isEmpty()) {
                log.info("No enabled channels for user {} notification type {}",
                        request.getRecipientId(), request.getType());
                return null;
            }

            String correlationId = request.getCorrelationId() != null
                    ? request.getCorrelationId() : UUID.randomUUID().toString();

            Map<String, Object> context = request.getContextData() != null
                    ? request.getContextData() : Map.of();

            RenderedContent rendered = null;
            String subject = request.getSubject();
            String body = request.getBody();
            String htmlBody = request.getHtmlBody();

            if (template != null) {
                rendered = templateService.render(template, request.getLocale(), context);
                if (subject == null) subject = rendered.subject();
                if (body == null) body = rendered.body();
                if (htmlBody == null) htmlBody = rendered.htmlBody();
            }

            Notification notification = Notification.builder()
                    .type(request.getType() != null ? request.getType() : NotificationType.CUSTOM)
                    .status(NotificationStatus.PENDING)
                    .priority(request.getPriority() != null ? request.getPriority() : NotificationPriority.MEDIUM)
                    .subject(subject)
                    .body(body)
                    .htmlBody(htmlBody)
                    .recipientId(request.getRecipientId())
                    .recipientEmail(request.getRecipientEmail())
                    .recipientPhone(request.getRecipientPhone())
                    .recipientName(request.getRecipientName())
                    .senderId(request.getSenderId())
                    .senderName(request.getSenderName())
                    .channels(channels)
                    .locale(request.getLocale())
                    .maxRetries(request.getMaxRetries() != null ? request.getMaxRetries() : 3)
                    .contextData(context)
                    .templateId(template != null ? template.getId() : null)
                    .workflowId(request.getWorkflowId())
                    .workflowExecutionId(request.getWorkflowExecutionId())
                    .requestId(request.getRequestId())
                    .correlationId(correlationId)
                    .expiresAt(request.getExpiresAt())
                    .build();

            Notification saved = notificationRepository.save(notification);

            if (request.getScheduleAt() != null && request.getScheduleAt().isAfter(Instant.now())) {
                saved.setStatus(NotificationStatus.PENDING);
                notificationRepository.save(saved);
                log.info("Notification scheduled: id={} at={}", saved.getId(), request.getScheduleAt());
                return saved;
            }

            saved.setStatus(NotificationStatus.QUEUED);
            saved = notificationRepository.save(saved);

            deliveryService.deliver(saved, context);

            return notificationRepository.findById(saved.getId()).orElse(saved);

        } catch (Exception e) {
            log.error("Failed to process notification: {}", e.getMessage(), e);
            return null;
        }
    }

    private List<NotificationChannel> resolveChannels(SendNotificationRequest request,
                                                       NotificationTemplate template) {
        if (request.getChannels() != null && !request.getChannels().isEmpty()) {
            return request.getChannels();
        }
        if (template != null && template.getSupportedChannels() != null) {
            return template.getSupportedChannels();
        }
        return List.of(NotificationChannel.EMAIL, NotificationChannel.IN_APP);
    }

    private List<NotificationChannel> filterByPreference(List<NotificationChannel> channels,
                                                          NotificationType type,
                                                          NotificationPreference preference) {
        List<NotificationChannel> filtered = new ArrayList<>();

        if (preference.getMutedTypes() != null && type != null
                && preference.getMutedTypes().contains(type.name())) {
            return List.of();
        }

        if (preference.getTypeOverrides() != null && type != null) {
            Boolean override = preference.getTypeOverrides().get(type.name());
            if (Boolean.FALSE.equals(override)) {
                return List.of();
            }
        }

        for (NotificationChannel ch : channels) {
            boolean enabled = switch (ch) {
                case EMAIL -> preference.isEmailEnabled();
                case PUSH -> preference.isPushEnabled();
                case SMS -> preference.isSmsEnabled();
                case SLACK -> preference.isSlackEnabled();
                case TEAMS -> preference.isTeamsEnabled();
                case IN_APP -> preference.isInAppEnabled();
            };
            if (enabled) filtered.add(ch);
        }
        return filtered;
    }
}
