package com.aiflow.enterprise.notification.mapper;

import com.aiflow.enterprise.notification.dto.NotificationPreferenceResponse;
import com.aiflow.enterprise.notification.dto.NotificationResponse;
import com.aiflow.enterprise.notification.dto.NotificationTemplateResponse;
import com.aiflow.enterprise.notification.entity.Notification;
import com.aiflow.enterprise.notification.entity.NotificationPreference;
import com.aiflow.enterprise.notification.entity.NotificationTemplate;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    List<NotificationResponse> toResponseList(List<Notification> notifications);

    NotificationTemplateResponse toTemplateResponse(NotificationTemplate template);

    List<NotificationTemplateResponse> toTemplateResponseList(List<NotificationTemplate> templates);

    NotificationPreferenceResponse toPreferenceResponse(NotificationPreference preference);

    List<NotificationPreferenceResponse> toPreferenceResponseList(List<NotificationPreference> preferences);
}
