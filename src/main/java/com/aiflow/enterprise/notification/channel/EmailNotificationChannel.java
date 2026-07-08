package com.aiflow.enterprise.notification.channel;

import com.aiflow.enterprise.notification.entity.Notification;
import com.aiflow.enterprise.notification.entity.embedded.DeliveryAttempt;
import com.aiflow.enterprise.notification.enums.DeliveryStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class EmailNotificationChannel implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    private final JavaMailSender mailSender;

    public EmailNotificationChannel(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public com.aiflow.enterprise.notification.enums.NotificationChannel getChannelType() {
        return com.aiflow.enterprise.notification.enums.NotificationChannel.EMAIL;
    }

    @Override
    public boolean isAvailable() {
        return mailSender != null;
    }

    @Override
    public DeliveryAttempt send(Notification notification, Map<String, Object> context) {
        long start = System.currentTimeMillis();
        DeliveryAttempt.DeliveryAttemptBuilder attempt = DeliveryAttempt.builder()
                .attemptNumber(notification.getRetryCount() + 1)
                .attemptedAt(Instant.now())
                .channel("EMAIL");

        try {
            String to = notification.getRecipientEmail();
            if (to == null || to.isBlank()) {
                return attempt.status(DeliveryStatus.FAILED)
                        .errorMessage("No recipient email address")
                        .durationMs(System.currentTimeMillis() - start)
                        .build();
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(notification.getSubject() != null ? notification.getSubject() : "Notification");

            if (notification.getHtmlBody() != null) {
                helper.setText(notification.getBody(), notification.getHtmlBody());
            } else {
                helper.setText(notification.getBody() != null ? notification.getBody() : "");
            }

            mailSender.send(mimeMessage);

            log.info("Email sent to {}: subject={}", to, notification.getSubject());
            return attempt.status(DeliveryStatus.DELIVERED)
                    .responseCode("200")
                    .durationMs(System.currentTimeMillis() - start)
                    .build();

        } catch (MessagingException e) {
            log.error("Email send failed to {}: {}", notification.getRecipientEmail(), e.getMessage());
            return attempt.status(DeliveryStatus.FAILED)
                    .errorMessage(e.getMessage())
                    .durationMs(System.currentTimeMillis() - start)
                    .build();
        }
    }
}
