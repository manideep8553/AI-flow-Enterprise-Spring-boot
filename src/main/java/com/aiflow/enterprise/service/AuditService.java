package com.aiflow.enterprise.service;

import com.aiflow.enterprise.entity.LoginAudit;
import com.aiflow.enterprise.repository.LoginAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final LoginAuditRepository loginAuditRepository;

    public AuditService(LoginAuditRepository loginAuditRepository) {
        this.loginAuditRepository = loginAuditRepository;
    }

    public void recordLoginEvent(String userId, String email, String username,
                                  boolean success, String failureReason,
                                  String ipAddress, String deviceId, String deviceName,
                                  String userAgent, String authMethod, String sessionId) {
        try {
            LoginAudit audit = LoginAudit.builder()
                    .userId(userId)
                    .email(email)
                    .username(username)
                    .action(success ? "LOGIN_SUCCESS" : "LOGIN_FAILED")
                    .success(success)
                    .failureReason(failureReason)
                    .ipAddress(ipAddress)
                    .deviceId(deviceId)
                    .deviceName(deviceName)
                    .userAgent(userAgent)
                    .timestamp(Instant.now())
                    .authMethod(authMethod)
                    .sessionId(sessionId)
                    .build();
            loginAuditRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to record login audit event: {}", e.getMessage());
        }
    }

    public void recordLogoutEvent(String userId, String email, String username,
                                   String ipAddress, String deviceName, String sessionId) {
        try {
            LoginAudit audit = LoginAudit.builder()
                    .userId(userId)
                    .email(email)
                    .username(username)
                    .action("LOGOUT")
                    .success(true)
                    .ipAddress(ipAddress)
                    .deviceName(deviceName)
                    .timestamp(Instant.now())
                    .authMethod("jwt")
                    .sessionId(sessionId)
                    .build();
            loginAuditRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to record logout audit event: {}", e.getMessage());
        }
    }

    public void recordPasswordResetEvent(String userId, String email) {
        try {
            LoginAudit audit = LoginAudit.builder()
                    .userId(userId)
                    .email(email)
                    .action("PASSWORD_RESET")
                    .success(true)
                    .timestamp(Instant.now())
                    .authMethod("token")
                    .build();
            loginAuditRepository.save(audit);
        } catch (Exception e) {
            log.error("Failed to record password reset audit event: {}", e.getMessage());
        }
    }
}
