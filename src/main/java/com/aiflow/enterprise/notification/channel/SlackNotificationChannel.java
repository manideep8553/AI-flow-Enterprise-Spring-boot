package com.aiflow.enterprise.notification.channel;

import com.aiflow.enterprise.notification.entity.Notification;
import com.aiflow.enterprise.notification.entity.embedded.DeliveryAttempt;
import com.aiflow.enterprise.notification.enums.DeliveryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Component
public class SlackNotificationChannel implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SlackNotificationChannel.class);

    private final RestTemplate restTemplate;

    public SlackNotificationChannel(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public com.aiflow.enterprise.notification.enums.NotificationChannel getChannelType() {
        return com.aiflow.enterprise.notification.enums.NotificationChannel.SLACK;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public DeliveryAttempt send(Notification notification, Map<String, Object> context) {
        long start = System.currentTimeMillis();
        DeliveryAttempt.DeliveryAttemptBuilder attempt = DeliveryAttempt.builder()
                .attemptNumber(notification.getRetryCount() + 1)
                .attemptedAt(Instant.now())
                .channel("SLACK");

        try {
            String webhookUrl = notification.getRecipientEmail();
            if (webhookUrl == null || webhookUrl.isBlank()) {
                return attempt.status(DeliveryStatus.FAILED)
                        .errorMessage("No Slack webhook URL configured")
                        .durationMs(System.currentTimeMillis() - start)
                        .build();
            }

            Map<String, Object> payload = Map.of(
                    "text", notification.getBody() != null ? notification.getBody() : "",
                    "username", "AIFlow Notifications"
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(webhookUrl, entity, String.class);

            log.info("Slack notification sent to webhook");
            return attempt.status(DeliveryStatus.DELIVERED)
                    .responseCode("200")
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.error("Slack notification failed: {}", e.getMessage());
            return attempt.status(DeliveryStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }
    }
}
