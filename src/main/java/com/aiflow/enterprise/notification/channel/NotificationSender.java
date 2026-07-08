package com.aiflow.enterprise.notification.channel;

import com.aiflow.enterprise.notification.entity.Notification;
import com.aiflow.enterprise.notification.entity.embedded.DeliveryAttempt;
import com.aiflow.enterprise.notification.enums.NotificationChannel;

import java.util.Map;

public interface NotificationSender {

    NotificationChannel getChannelType();

    boolean isAvailable();

    DeliveryAttempt send(Notification notification, Map<String, Object> context);
}
