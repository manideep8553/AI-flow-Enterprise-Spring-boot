package com.aiflow.enterprise.notification.channel;

import com.aiflow.enterprise.notification.entity.Notification;
import com.aiflow.enterprise.notification.entity.embedded.DeliveryAttempt;
import com.aiflow.enterprise.notification.enums.DeliveryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class InAppNotificationChannel implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(InAppNotificationChannel.class);

    @Override
    public com.aiflow.enterprise.notification.enums.NotificationChannel getChannelType() {
        return com.aiflow.enterprise.notification.enums.NotificationChannel.IN_APP;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public DeliveryAttempt send(Notification notification, Map<String, Object> context) {
        long start = System.currentTimeMillis();

        log.info("In-app notification for user {}: {}", notification.getRecipientId(), notification.getSubject());

        return DeliveryAttempt.builder()
                .attemptNumber(notification.getRetryCount() + 1)
                .attemptedAt(Instant.now())
                .channel("IN_APP")
                .status(DeliveryStatus.DELIVERED)
                .responseCode("200")
                .durationMs(System.currentTimeMillis() - start)
                .build();
    }
}
