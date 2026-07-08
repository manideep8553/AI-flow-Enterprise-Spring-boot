package com.aiflow.enterprise.service;

import com.aiflow.enterprise.entity.RefreshToken;
import com.aiflow.enterprise.entity.UserSession;
import com.aiflow.enterprise.repository.RefreshTokenRepository;
import com.aiflow.enterprise.repository.UserSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository userSessionRepository;

    public TokenService(RefreshTokenRepository refreshTokenRepository,
                        UserSessionRepository userSessionRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userSessionRepository = userSessionRepository;
    }

    public RefreshToken storeRefreshToken(String token, String userId, String username,
                                          String deviceId, String deviceName,
                                          String ipAddress, String userAgent,
                                          long expirationMs) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .userId(userId)
                .username(username)
                .deviceId(deviceId)
                .deviceName(deviceName)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(Instant.now().plusMillis(expirationMs))
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> validateRefreshToken(String token) {
        return refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .filter(rt -> rt.getExpiresAt().isAfter(Instant.now()));
    }

    public RefreshToken rotateRefreshToken(String oldToken, String newToken, long expirationMs) {
        RefreshToken existing = refreshTokenRepository.findByToken(oldToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        existing.setRevoked(true);
        existing.setRevokedAt(Instant.now());
        existing.setReplacedByToken(newToken);
        refreshTokenRepository.save(existing);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .token(newToken)
                .userId(existing.getUserId())
                .username(existing.getUsername())
                .deviceId(existing.getDeviceId())
                .deviceName(existing.getDeviceName())
                .ipAddress(existing.getIpAddress())
                .userAgent(existing.getUserAgent())
                .expiresAt(Instant.now().plusMillis(expirationMs))
                .previousToken(oldToken)
                .build();
        return refreshTokenRepository.save(newRefreshToken);
    }

    public void revokeAllUserTokens(String userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(userId);
        tokens.forEach(t -> {
            t.setRevoked(true);
            t.setRevokedAt(Instant.now());
        });
        refreshTokenRepository.saveAll(tokens);

        List<UserSession> sessions = userSessionRepository.findByUserIdAndActiveTrue(userId);
        sessions.forEach(s -> {
            s.setActive(false);
            s.setLoggedOutAt(Instant.now());
        });
        userSessionRepository.saveAll(sessions);

        log.info("All tokens and sessions revoked for user: {}", userId);
    }

    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            rt.setRevokedAt(Instant.now());
            refreshTokenRepository.save(rt);
        });
    }

    public UserSession createSession(String userId, String sessionToken, String refreshToken,
                                     String ipAddress, String deviceId, String deviceName,
                                     String userAgent, Instant expiresAt) {
        UserSession session = UserSession.builder()
                .userId(userId)
                .sessionToken(sessionToken)
                .refreshToken(refreshToken)
                .active(true)
                .lastActivityAt(Instant.now())
                .expiresAt(expiresAt)
                .ipAddress(ipAddress)
                .deviceId(deviceId)
                .deviceName(deviceName)
                .userAgent(userAgent)
                .build();
        return userSessionRepository.save(session);
    }

    public void endSession(String sessionToken) {
        userSessionRepository.findBySessionTokenAndActiveTrue(sessionToken).ifPresent(s -> {
            s.setActive(false);
            s.setLoggedOutAt(Instant.now());
            userSessionRepository.save(s);
        });
    }

    public List<UserSession> getActiveSessions(String userId) {
        return userSessionRepository.findByUserIdAndActiveTrue(userId);
    }

    @Transactional(readOnly = true)
    public long getActiveSessionCount(String userId) {
        return userSessionRepository.countByUserIdAndActiveTrue(userId);
    }
}
