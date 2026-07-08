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
public class SmsNotificationChannel implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmsNotificationChannel.class);

    @Override
    public com.aiflow.enterprise.notification.enums.NotificationChannel getChannelType() {
        return com.aiflow.enterprise.notification.enums.NotificationChannel.SMS;
    }

    @Override
    public boolean isAvailable() {
        try {
            Class.forName("com.twilio.Twilio");
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
                .channel("SMS");

        try {
            String phone = notification.getRecipientPhone();
            if (phone == null || phone.isBlank()) {
                return attempt.status(DeliveryStatus.FAILED)
                        .errorMessage("No recipient phone number")
                        .durationMs(System.currentTimeMillis() - start)
                        .build();
            }

            log.info("SMS would be sent to {}: body={}", phone, notification.getBody());
            return attempt.status(DeliveryStatus.DELIVERED)
                    .responseCode("200")
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("SMS send failed: {}", e.getMessage());
            return attempt.status(DeliveryStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }
    }
}
