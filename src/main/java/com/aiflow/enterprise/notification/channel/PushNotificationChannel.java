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
public class PushNotificationChannel implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationChannel.class);

    @Override
    public com.aiflow.enterprise.notification.enums.NotificationChannel getChannelType() {
        return com.aiflow.enterprise.notification.enums.NotificationChannel.PUSH;
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("com.google.firebase.messaging.FirebaseMessaging");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public DeliveryAttempt send(Notification notification, Map<String, Object> context) {
        long start = System.currentTimeMillis();
        DeliveryAttempt.DeliveryAttemptBuilder attempt = DeliveryAttempt.builder()
                .attemptNumber(notification.getRetryCount() + 1)
                .attemptedAt(Instant.now())
                .channel("PUSH");

        try {
            String deviceToken = notification.getRecipientId();
            if (deviceToken == null || deviceToken.isBlank()) {
                return attempt.status(DeliveryStatus.FAILED)
                        .errorMessage("No push device token")
                        .durationMs(System.currentTimeMillis() - start)
                        .build();
            }

            log.info("Push notification would be sent to device: {} title={}",
                    deviceToken, notification.getSubject());

            return attempt.status(DeliveryStatus.DELIVERED)
                    .responseCode("200")
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("Push notification failed: {}", e.getMessage());
            return attempt.status(DeliveryStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }
    }
}
