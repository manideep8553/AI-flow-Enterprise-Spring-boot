package com.aiflow.enterprise.notification.service;

import com.aiflow.enterprise.notification.dto.NotificationPreferenceRequest;
import com.aiflow.enterprise.notification.dto.NotificationPreferenceResponse;
import com.aiflow.enterprise.notification.dto.NotificationResponse;
import com.aiflow.enterprise.notification.dto.NotificationStatsResponse;
import com.aiflow.enterprise.notification.dto.NotificationTemplateRequest;
import com.aiflow.enterprise.notification.dto.NotificationTemplateResponse;
import com.aiflow.enterprise.notification.dto.SendNotificationRequest;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface NotificationService {

    NotificationResponse sendNotification(SendNotificationRequest request);

    NotificationResponse sendAndPublish(SendNotificationRequest request);

    NotificationResponse getNotificationById(String id);

    Page<NotificationResponse> getNotifications(int page, int size, String recipientId,
                                                  String type, String status);

    NotificationResponse markAsRead(String id);

    long getUnreadCount(String recipientId);

    NotificationStatsResponse getStats();

    NotificationTemplateResponse createTemplate(NotificationTemplateRequest request);

    NotificationTemplateResponse updateTemplate(String id, NotificationTemplateRequest request);

    NotificationTemplateResponse getTemplateById(String id);

    Page<NotificationTemplateResponse> getTemplates(int page, int size, String type);

    void deleteTemplate(String id);

    NotificationPreferenceResponse getPreferences(String userId);

    NotificationPreferenceResponse updatePreferences(String userId, NotificationPreferenceRequest request);

    Map<String, Object> renderTemplatePreview(String templateId, Map<String, Object> context, String locale);
}
