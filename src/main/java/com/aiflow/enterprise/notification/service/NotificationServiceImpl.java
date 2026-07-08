package com.aiflow.enterprise.notification.service;

import com.aiflow.enterprise.notification.dto.NotificationPreferenceRequest;
import com.aiflow.enterprise.notification.dto.NotificationPreferenceResponse;
import com.aiflow.enterprise.notification.dto.NotificationResponse;
import com.aiflow.enterprise.notification.dto.NotificationStatsResponse;
import com.aiflow.enterprise.notification.dto.NotificationTemplateRequest;
import com.aiflow.enterprise.notification.dto.NotificationTemplateResponse;
import com.aiflow.enterprise.notification.dto.SendNotificationRequest;
import com.aiflow.enterprise.notification.entity.Notification;
import com.aiflow.enterprise.notification.entity.NotificationPreference;
import com.aiflow.enterprise.notification.entity.NotificationTemplate;
import com.aiflow.enterprise.notification.enums.NotificationStatus;
import com.aiflow.enterprise.notification.enums.NotificationType;
import com.aiflow.enterprise.notification.mapper.NotificationMapper;
import com.aiflow.enterprise.notification.repository.DeliveryRecordRepository;
import com.aiflow.enterprise.notification.repository.NotificationPreferenceRepository;
import com.aiflow.enterprise.notification.repository.NotificationRepository;
import com.aiflow.enterprise.notification.repository.NotificationTemplateRepository;
import com.aiflow.enterprise.notification.service.NotificationTemplateService.RenderedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final DeliveryRecordRepository deliveryRecordRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationOrchestrator orchestrator;
    private final NotificationEventPublisher eventPublisher;
    private final NotificationTemplateService templateService;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   NotificationTemplateRepository templateRepository,
                                   NotificationPreferenceRepository preferenceRepository,
                                   DeliveryRecordRepository deliveryRecordRepository,
                                   NotificationMapper notificationMapper,
                                   NotificationOrchestrator orchestrator,
                                   NotificationEventPublisher eventPublisher,
                                   NotificationTemplateService templateService) {
        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
        this.preferenceRepository = preferenceRepository;
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.notificationMapper = notificationMapper;
        this.orchestrator = orchestrator;
        this.eventPublisher = eventPublisher;
        this.templateService = templateService;
    }

    @Override
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        Notification notification = orchestrator.processNotificationRequest(request);
        return notification != null ? notificationMapper.toResponse(notification) : null;
    }

    @Override
    public NotificationResponse sendAndPublish(SendNotificationRequest request) {
        NotificationResponse response = sendNotification(request);
        eventPublisher.publish(request);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(String id) {
        Notification notification = notificationRepository.findById(id).orElse(null);
        return notification != null ? notificationMapper.toResponse(notification) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(int page, int size, String recipientId,
                                                        String type, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Notification> notifications;
        if (recipientId != null) {
            notifications = notificationRepository.findByRecipientId(recipientId, pageable);
        } else if (type != null) {
            notifications = notificationRepository.findByType(
                    NotificationType.valueOf(type.toUpperCase()), pageable);
        } else if (status != null) {
            notifications = notificationRepository.findByStatus(
                    NotificationStatus.valueOf(status.toUpperCase()), pageable);
        } else {
            notifications = notificationRepository.findAll(pageable);
        }

        return notifications.map(notificationMapper::toResponse);
    }

    @Override
    public NotificationResponse markAsRead(String id) {
        Notification notification = notificationRepository.findById(id).orElse(null);
        if (notification != null && !notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
        return notification != null ? notificationMapper.toResponse(notification) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(String recipientId) {
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationStatsResponse getStats() {
        return NotificationStatsResponse.builder()
                .total(notificationRepository.count())
                .sent(notificationRepository.countByStatus(NotificationStatus.SENT))
                .delivered(notificationRepository.countByStatus(NotificationStatus.DELIVERED))
                .failed(notificationRepository.countByStatus(NotificationStatus.FAILED))
                .pending(notificationRepository.countByStatus(NotificationStatus.PENDING))
                .retrying(notificationRepository.countByStatus(NotificationStatus.RETRYING))
                .unread(0)
                .build();
    }

    @Override
    public NotificationTemplateResponse createTemplate(NotificationTemplateRequest request) {
        NotificationTemplate template = NotificationTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .supportedChannels(request.getSupportedChannels())
                .subjectTemplate(request.getSubjectTemplate())
                .bodyTemplate(request.getBodyTemplate())
                .htmlBodyTemplate(request.getHtmlBodyTemplate())
                .pushTitleTemplate(request.getPushTitleTemplate())
                .pushBodyTemplate(request.getPushBodyTemplate())
                .smsBodyTemplate(request.getSmsBodyTemplate())
                .localizedContents(request.getLocalizedContents())
                .defaultContext(request.getDefaultContext())
                .requiredContextKeys(request.getRequiredContextKeys())
                .enabled(request.isEnabled())
                .build();

        NotificationTemplate saved = templateRepository.save(template);
        return notificationMapper.toTemplateResponse(saved);
    }

    @Override
    public NotificationTemplateResponse updateTemplate(String id, NotificationTemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(id).orElse(null);
        if (template == null) return null;

        if (request.getName() != null) template.setName(request.getName());
        if (request.getDescription() != null) template.setDescription(request.getDescription());
        if (request.getType() != null) template.setType(request.getType());
        if (request.getSupportedChannels() != null) template.setSupportedChannels(request.getSupportedChannels());
        if (request.getSubjectTemplate() != null) template.setSubjectTemplate(request.getSubjectTemplate());
        if (request.getBodyTemplate() != null) template.setBodyTemplate(request.getBodyTemplate());
        if (request.getHtmlBodyTemplate() != null) template.setHtmlBodyTemplate(request.getHtmlBodyTemplate());
        if (request.getPushTitleTemplate() != null) template.setPushTitleTemplate(request.getPushTitleTemplate());
        if (request.getPushBodyTemplate() != null) template.setPushBodyTemplate(request.getPushBodyTemplate());
        if (request.getSmsBodyTemplate() != null) template.setSmsBodyTemplate(request.getSmsBodyTemplate());
        if (request.getLocalizedContents() != null) template.setLocalizedContents(request.getLocalizedContents());
        if (request.getDefaultContext() != null) template.setDefaultContext(request.getDefaultContext());
        if (request.getRequiredContextKeys() != null) template.setRequiredContextKeys(request.getRequiredContextKeys());
        template.setEnabled(request.isEnabled());

        NotificationTemplate saved = templateRepository.save(template);
        return notificationMapper.toTemplateResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationTemplateResponse getTemplateById(String id) {
        NotificationTemplate template = templateRepository.findById(id).orElse(null);
        return template != null ? notificationMapper.toTemplateResponse(template) : null;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationTemplateResponse> getTemplates(int page, int size, String type) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<NotificationTemplate> templates;
        if (type != null) {
            templates = templateRepository.findByType(NotificationType.valueOf(type.toUpperCase()), pageable);
        } else {
            templates = templateRepository.findAll(pageable);
        }
        return templates.map(notificationMapper::toTemplateResponse);
    }

    @Override
    public void deleteTemplate(String id) {
        templateRepository.findById(id).ifPresent(templateRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getPreferences(String userId) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId).orElse(null);
        if (preference == null) {
            preference = NotificationPreference.builder()
                    .userId(userId)
                    .emailEnabled(true)
                    .pushEnabled(true)
                    .smsEnabled(false)
                    .slackEnabled(false)
                    .teamsEnabled(false)
                    .inAppEnabled(true)
                    .build();
            preference = preferenceRepository.save(preference);
        }
        return notificationMapper.toPreferenceResponse(preference);
    }

    @Override
    public NotificationPreferenceResponse updatePreferences(String userId, NotificationPreferenceRequest request) {
        NotificationPreference preference = preferenceRepository.findByUserId(userId)
                .orElse(NotificationPreference.builder().userId(userId).build());

        preference.setEmailEnabled(request.isEmailEnabled());
        preference.setPushEnabled(request.isPushEnabled());
        preference.setSmsEnabled(request.isSmsEnabled());
        preference.setSlackEnabled(request.isSlackEnabled());
        preference.setTeamsEnabled(request.isTeamsEnabled());
        preference.setInAppEnabled(request.isInAppEnabled());
        preference.setEmailAddress(request.getEmailAddress());
        preference.setPhoneNumber(request.getPhoneNumber());
        preference.setSlackWebhook(request.getSlackWebhook());
        preference.setTeamsWebhook(request.getTeamsWebhook());
        preference.setPushDeviceToken(request.getPushDeviceToken());
        preference.setTypeOverrides(request.getTypeOverrides());
        preference.setMutedTypes(request.getMutedTypes());
        preference.setQuietHoursEnabled(request.isQuietHoursEnabled());
        preference.setQuietHoursStart(request.getQuietHoursStart());
        preference.setQuietHoursEnd(request.getQuietHoursEnd());
        preference.setDigestFrequency(request.getDigestFrequency());
        preference.setDigestTypes(request.getDigestTypes());
        preference.setLocale(request.getLocale());
        preference.setDoNotDisturb(request.isDoNotDisturb());

        NotificationPreference saved = preferenceRepository.save(preference);
        return notificationMapper.toPreferenceResponse(saved);
    }

    @Override
    public Map<String, Object> renderTemplatePreview(String templateId, Map<String, Object> context,
                                                      String locale) {
        NotificationTemplate template = templateRepository.findById(templateId).orElse(null);
        if (template == null) return Map.of("error", "Template not found");

        var validation = templateService.validateContextKeys(template, context);
        RenderedContent rendered = templateService.render(template, locale, context);

        Map<String, Object> preview = new HashMap<>();
        preview.put("subject", rendered.subject());
        preview.put("body", rendered.body());
        preview.put("htmlBody", rendered.htmlBody());
        preview.put("pushTitle", rendered.pushTitle());
        preview.put("pushBody", rendered.pushBody());
        preview.put("smsBody", rendered.smsBody());
        preview.putAll(validation);
        return preview;
    }
}
