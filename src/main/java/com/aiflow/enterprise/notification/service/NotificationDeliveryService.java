package com.aiflow.enterprise.notification.service;

import com.aiflow.enterprise.notification.channel.ChannelFactory;
import com.aiflow.enterprise.notification.channel.NotificationSender;
import com.aiflow.enterprise.notification.entity.DeliveryRecord;
import com.aiflow.enterprise.notification.entity.Notification;
import com.aiflow.enterprise.notification.entity.embedded.DeliveryAttempt;
import com.aiflow.enterprise.notification.enums.DeliveryStatus;
import com.aiflow.enterprise.notification.enums.NotificationChannel;
import com.aiflow.enterprise.notification.enums.NotificationStatus;
import com.aiflow.enterprise.notification.repository.DeliveryRecordRepository;
import com.aiflow.enterprise.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class NotificationDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);

    private final ChannelFactory channelFactory;
    private final DeliveryRecordRepository deliveryRecordRepository;
    private final NotificationRepository notificationRepository;

    public NotificationDeliveryService(ChannelFactory channelFactory,
                                       DeliveryRecordRepository deliveryRecordRepository,
                                       NotificationRepository notificationRepository) {
        this.channelFactory = channelFactory;
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.notificationRepository = notificationRepository;
    }

    public void deliver(Notification notification, Map<String, Object> context) {
        List<NotificationChannel> channels = notification.getChannels();
        if (channels == null || channels.isEmpty()) {
            log.warn("No channels configured for notification {}", notification.getId());
            return;
        }

        List<DeliveryAttempt> allAttempts = new ArrayList<>();
        boolean allDelivered = true;

        for (NotificationChannel ch : channels) {
            try {
                if (!channelFactory.isChannelAvailable(ch)) {
                    log.warn("Channel {} is not available for notification {}", ch, notification.getId());
                    allDelivered = false;
                    continue;
                }

                NotificationSender sender = channelFactory.getSender(ch);
                DeliveryAttempt attempt = sender.send(notification, context);
                allAttempts.add(attempt);

                saveDeliveryRecord(notification, ch, attempt);

                if (attempt.getStatus() != DeliveryStatus.DELIVERED) {
                    allDelivered = false;
                }

            } catch (Exception e) {
                log.error("Delivery failed for channel {} on notification {}: {}",
                        ch, notification.getId(), e.getMessage());
                allDelivered = false;

                DeliveryAttempt failedAttempt = DeliveryAttempt.builder()
                        .attemptNumber(notification.getRetryCount() + 1)
                        .attemptedAt(Instant.now())
                        .channel(ch.name())
                        .status(DeliveryStatus.FAILED)
                        .errorMessage(e.getMessage())
                        .durationMs(0)
                        .build();
                allAttempts.add(failedAttempt);
                saveDeliveryRecord(notification, ch, failedAttempt);
            }
        }

        updateNotificationStatus(notification, allDelivered, allAttempts);
    }

    public void retryDelivery(Notification notification) {
        if (notification.getRetryCount() >= notification.getMaxRetries()) {
            log.warn("Notification {} exceeded max retries ({})", notification.getId(), notification.getMaxRetries());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailedAt(Instant.now());
            notification.setLastError("Max retries exceeded");
            notificationRepository.save(notification);
            return;
        }

        notification.setStatus(NotificationStatus.RETRYING);
        notification.setRetryCount(notification.getRetryCount() + 1);
        notification.setNextRetryAt(calculateNextRetry(notification.getRetryCount()));
        notificationRepository.save(notification);

        deliver(notification, notification.getContextData());
    }

    public void sendToDlq(Notification notification) {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setFailedAt(Instant.now());
        notification.setLastError("Sent to dead letter queue after " + notification.getRetryCount() + " retries");
        notificationRepository.save(notification);

        log.warn("Notification {} sent to DLQ", notification.getId());
    }

    private void saveDeliveryRecord(Notification notification, NotificationChannel channel,
                                     DeliveryAttempt attempt) {
        DeliveryRecord record = DeliveryRecord.builder()
                .notificationId(notification.getId())
                .channel(channel)
                .recipientAddress(resolveRecipient(notification, channel))
                .status(attempt.getStatus())
                .attemptCount(notification.getRetryCount() + 1)
                .maxRetries(notification.getMaxRetries())
                .lastAttemptAt(Instant.now())
                .deliveredAt(attempt.getStatus() == DeliveryStatus.DELIVERED ? Instant.now() : null)
                .lastError(attempt.getErrorMessage())
                .attempts(List.of(attempt))
                .dlq(false)
                .build();

        if (attempt.getStatus() == DeliveryStatus.FAILED
                && notification.getRetryCount() >= notification.getMaxRetries()) {
            record.setDlq(true);
            record.setDlqAt(Instant.now());
            record.setDlqReason("Max retries exceeded");
        }

        deliveryRecordRepository.save(record);
    }

    private void updateNotificationStatus(Notification notification, boolean allDelivered,
                                           List<DeliveryAttempt> attempts) {
        if (allDelivered) {
            notification.setStatus(NotificationStatus.DELIVERED);
            notification.setDeliveredAt(Instant.now());
        } else {
            boolean anyDelivered = attempts.stream()
                    .anyMatch(a -> a.getStatus() == DeliveryStatus.DELIVERED);
            if (anyDelivered) {
                notification.setStatus(NotificationStatus.PARTIALLY_DELIVERED);
            }
        }
        notification.setDeliveryAttempts(attempts);
        notification.setLastError(attempts.stream()
                .filter(a -> a.getStatus() == DeliveryStatus.FAILED)
                .map(DeliveryAttempt::getErrorMessage)
                .reduce((a, b) -> a + "; " + b)
                .orElse(null));
        notificationRepository.save(notification);
    }

    private Instant calculateNextRetry(int retryCount) {
        return Instant.now().plusSeconds((long) Math.pow(2, retryCount) * 30);
    }

    private String resolveRecipient(Notification notification, NotificationChannel channel) {
        return switch (channel) {
            case EMAIL -> notification.getRecipientEmail();
            case SMS -> notification.getRecipientPhone();
            case PUSH -> notification.getRecipientId();
            case SLACK, TEAMS -> notification.getRecipientEmail();
            case IN_APP -> notification.getRecipientId();
        };
    }
}
