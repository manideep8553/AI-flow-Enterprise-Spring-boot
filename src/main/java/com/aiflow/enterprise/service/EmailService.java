package com.aiflow.enterprise.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String appName;
    private final String appUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String fromAddress,
                        @Value("${app.name}") String appName,
                        @Value("${app.frontend.url}") String appUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.appName = appName;
        this.appUrl = appUrl;
    }

    @Async
    public void sendEmailVerification(String to, String token) {
        String subject = "Verify your email address - " + appName;
        String verificationLink = appUrl + "/verify-email?token=" + token;
        String message = String.format("""
                Hello,
                
                Thank you for registering with %s.
                
                Please click the link below to verify your email address:
                %s
                
                This link will expire in 24 hours.
                
                If you did not create an account, please ignore this email.
                
                Best regards,
                %s Team
                """, appName, verificationLink, appName);

        sendEmail(to, subject, message);
    }

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        String subject = "Reset your password - " + appName;
        String resetLink = appUrl + "/reset-password?token=" + token;
        String message = String.format("""
                Hello,
                
                We received a request to reset your password for %s.
                
                Please click the link below to reset your password:
                %s
                
                This link will expire in 1 hour.
                
                If you did not request a password reset, please ignore this email.
                
                Best regards,
                %s Team
                """, appName, resetLink, appName);

        sendEmail(to, subject, message);
    }

    @Async
    public void sendPasswordChangedNotification(String to) {
        String subject = "Your password has been changed - " + appName;
        String message = String.format("""
                Hello,
                
                Your password for %s has been successfully changed.
                
                If you did not make this change, please contact your administrator immediately.
                
                Best regards,
                %s Team
                """, appName, appName);

        sendEmail(to, subject, message);
    }

    @Async
    public void sendAccountLockedNotification(String to, String username, long minutes) {
        String subject = "Account locked - " + appName;
        String message = String.format("""
                Hello %s,
                
                Your account for %s has been temporarily locked due to multiple failed login attempts.
                
                Your account will be unlocked in %d minutes.
                
                If you did not attempt to log in, please contact your administrator immediately.
                
                Best regards,
                %s Team
                """, username, appName, minutes, appName);

        sendEmail(to, subject, message);
    }

    private void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
