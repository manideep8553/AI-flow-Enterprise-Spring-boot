package com.aiflow.enterprise.security;

import com.aiflow.enterprise.dto.response.AuthResponse;
import com.aiflow.enterprise.entity.LoginAudit;
import com.aiflow.enterprise.entity.RefreshToken;
import com.aiflow.enterprise.entity.UserSession;
import com.aiflow.enterprise.enums.AuthAction;
import com.aiflow.enterprise.repository.LoginAuditRepository;
import com.aiflow.enterprise.repository.RefreshTokenRepository;
import com.aiflow.enterprise.repository.UserSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Component
@Transactional
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final LoginAuditRepository loginAuditRepository;
    private final ObjectMapper objectMapper;
    private final String frontendRedirectUrl;

    public OAuth2SuccessHandler(JwtTokenProvider jwtTokenProvider,
                                RefreshTokenRepository refreshTokenRepository,
                                UserSessionRepository userSessionRepository,
                                LoginAuditRepository loginAuditRepository,
                                ObjectMapper objectMapper,
                                @Value("${app.frontend.redirect-url}") String frontendRedirectUrl) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userSessionRepository = userSessionRepository;
        this.loginAuditRepository = loginAuditRepository;
        this.objectMapper = objectMapper;
        this.frontendRedirectUrl = frontendRedirectUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String userId = userDetails.getId();
        String username = userDetails.getUsername();
        Set<String> roles = Set.of("ROLE_" + userDetails.getRole().name());

        String accessToken = jwtTokenProvider.generateAccessToken(userId, username, roles);
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(userId, username);

        String deviceId = UUID.randomUUID().toString();
        String userAgent = request.getHeader("User-Agent");

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshTokenValue)
                .userId(userId)
                .username(username)
                .deviceId(deviceId)
                .deviceName(parseDeviceName(userAgent))
                .ipAddress(request.getRemoteAddr())
                .userAgent(userAgent)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        UserSession session = UserSession.builder()
                .userId(userId)
                .sessionToken(UUID.randomUUID().toString())
                .refreshToken(refreshTokenValue)
                .active(true)
                .lastActivityAt(Instant.now())
                .expiresAt(refreshTokenEntity.getExpiresAt())
                .ipAddress(request.getRemoteAddr())
                .deviceId(deviceId)
                .deviceName(parseDeviceName(userAgent))
                .userAgent(userAgent)
                .build();
        userSessionRepository.save(session);

        loginAuditRepository.save(LoginAudit.builder()
                .userId(userId)
                .email(userDetails.getEmail())
                .username(username)
                .action(AuthAction.OAUTH2_LOGIN_SUCCESS.name())
                .success(true)
                .ipAddress(request.getRemoteAddr())
                .deviceName(parseDeviceName(userAgent))
                .userAgent(userAgent)
                .timestamp(Instant.now())
                .authMethod("oauth2_" + determineProvider(userDetails))
                .sessionId(session.getId())
                .build());

        log.info("OAuth2 login success: user={}, provider={}", username, determineProvider(userDetails));

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .userId(userId)
                .username(username)
                .email(userDetails.getEmail())
                .roles(roles)
                .build();

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getOutputStream(), authResponse);
    }

    private String parseDeviceName(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Mobile")) return "Mobile Device";
        if (userAgent.contains("Windows")) return "Windows PC";
        if (userAgent.contains("Mac")) return "Mac";
        if (userAgent.contains("Linux")) return "Linux";
        return "Unknown Device";
    }

    private String determineProvider(CustomUserDetails user) {
        if (user.getAttributes() != null && user.getAttributes().containsKey("sub")) {
            return "google";
        }
        return "microsoft";
    }
}
