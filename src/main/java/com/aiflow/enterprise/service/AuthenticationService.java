package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.request.ChangePasswordRequest;
import com.aiflow.enterprise.dto.request.ForgotPasswordRequest;
import com.aiflow.enterprise.dto.request.LoginRequest;
import com.aiflow.enterprise.dto.request.RefreshTokenRequest;
import com.aiflow.enterprise.dto.request.RegisterRequest;
import com.aiflow.enterprise.dto.request.ResetPasswordRequest;
import com.aiflow.enterprise.dto.response.AuthResponse;
import com.aiflow.enterprise.entity.RefreshToken;
import com.aiflow.enterprise.entity.User;
import com.aiflow.enterprise.entity.VerificationToken;
import com.aiflow.enterprise.enums.TokenType;
import com.aiflow.enterprise.exception.BadRequestException;
import com.aiflow.enterprise.exception.DuplicateResourceException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.repository.UserRepository;
import com.aiflow.enterprise.repository.VerificationTokenRepository;
import com.aiflow.enterprise.security.CustomUserDetails;
import com.aiflow.enterprise.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenService tokenService;
    private final LoginAttemptService loginAttemptService;
    private final EmailService emailService;
    private final AuditService auditService;

    public AuthenticationService(UserRepository userRepository,
                                 VerificationTokenRepository verificationTokenRepository,
                                 PasswordEncoder passwordEncoder,
                                 AuthenticationManager authenticationManager,
                                 JwtTokenProvider jwtTokenProvider,
                                 TokenService tokenService,
                                 LoginAttemptService loginAttemptService,
                                 EmailService emailService,
                                 AuditService auditService) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.tokenService = tokenService;
        this.loginAttemptService = loginAttemptService;
        this.emailService = emailService;
        this.auditService = auditService;
    }

    public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(request.getRole() != null ? request.getRole() : com.aiflow.enterprise.enums.UserRole.VIEWER)
                .active(true)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);

        String verificationToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        VerificationToken tokenEntity = VerificationToken.builder()
                .token(verificationToken)
                .userId(user.getId())
                .email(user.getEmail())
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .expiresAt(Instant.now().plus(java.time.Duration.ofHours(24)))
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        verificationTokenRepository.save(tokenEntity);

        emailService.sendEmailVerification(user.getEmail(), verificationToken);

        log.info("User registered: {} with email {}", user.getUsername(), user.getEmail());

        return generateAuthResponse(user, ipAddress, userAgent, "local");
    }

    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> userRepository.findByUsername(request.getEmail())
                        .orElseThrow(() -> {
                            auditService.recordLoginEvent(null, request.getEmail(), null,
                                    false, "User not found", ipAddress, null,
                                    parseDeviceName(userAgent), userAgent, "local", null);
                            return new BadCredentialsException("Invalid username or email");
                        }));

        loginAttemptService.checkIfAccountLocked(user);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            loginAttemptService.recordFailedAttempt(user.getId());
            auditService.recordLoginEvent(user.getId(), user.getEmail(), user.getUsername(),
                    false, "Invalid password", ipAddress, null,
                    parseDeviceName(userAgent), userAgent, "local", null);
            throw new BadCredentialsException("Invalid username or password");
        } catch (LockedException e) {
            throw new BadRequestException("Account is locked. Please try again later.");
        } catch (DisabledException e) {
            throw new BadRequestException("Account is disabled. Please contact administrator.");
        }

        loginAttemptService.resetFailedAttempts(user.getId());
        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(ipAddress);
        user.setLastLoginDevice(parseDeviceName(userAgent));
        userRepository.save(user);

        auditService.recordLoginEvent(user.getId(), user.getEmail(), user.getUsername(),
                true, null, ipAddress, null,
                parseDeviceName(userAgent), userAgent, "local", null);

        log.info("User logged in: {}", user.getUsername());
        return generateAuthResponse(user, ipAddress, userAgent, "local");
    }

    public AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress, String userAgent) {
        String oldTokenValue = request.getRefreshToken();

        RefreshToken oldToken = tokenService.validateRefreshToken(oldTokenValue)
                .orElseThrow(() -> {
                    log.warn("Invalid or expired refresh token attempt from IP: {}", ipAddress);
                    return new BadRequestException("Invalid or expired refresh token");
                });

        String userId = oldToken.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String newRefreshTokenValue = jwtTokenProvider.generateRefreshToken(userId, user.getUsername());
        tokenService.rotateRefreshToken(oldTokenValue, newRefreshTokenValue,
                jwtTokenProvider.getRefreshTokenExpirationMs());

        Set<String> roles = Set.of("ROLE_" + user.getRole().name());
        String newAccessToken = jwtTokenProvider.generateAccessToken(userId, user.getUsername(), roles);

        log.info("Token refreshed for user: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .userId(userId)
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    public void logout(String userId, String refreshToken, String ipAddress, String userAgent) {
        if (refreshToken != null) {
            tokenService.revokeToken(refreshToken);
        }
        tokenService.revokeAllUserTokens(userId);

        userRepository.findById(userId).ifPresent(user -> {
            auditService.recordLogoutEvent(userId, user.getEmail(), user.getUsername(),
                    ipAddress, parseDeviceName(userAgent), null);
            log.info("User logged out: {}", user.getUsername());
        });
    }

    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        if (verificationToken.isUsed()) {
            throw new BadRequestException("Verification token has already been used");
        }

        if (verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Verification token has expired");
        }

        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", verificationToken.getUserId()));

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);

        verificationToken.setUsed(true);
        verificationToken.setUsedAt(Instant.now());
        verificationTokenRepository.save(verificationToken);

        log.info("Email verified for user: {}", user.getUsername());
    }

    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        verificationTokenRepository.findByUserIdAndTokenTypeAndUsedFalse(
                        user.getId(), TokenType.EMAIL_VERIFICATION)
                .ifPresent(t -> {
                    t.setUsed(true);
                    t.setUsedAt(Instant.now());
                    verificationTokenRepository.save(t);
                });

        String newToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        VerificationToken tokenEntity = VerificationToken.builder()
                .token(newToken)
                .userId(user.getId())
                .email(user.getEmail())
                .tokenType(TokenType.EMAIL_VERIFICATION)
                .expiresAt(Instant.now().plus(java.time.Duration.ofHours(24)))
                .build();
        verificationTokenRepository.save(tokenEntity);

        emailService.sendEmailVerification(user.getEmail(), newToken);
        log.info("Verification email resent to: {}", email);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            verificationTokenRepository.findByUserIdAndTokenTypeAndUsedFalse(
                            user.getId(), TokenType.PASSWORD_RESET)
                    .ifPresent(t -> {
                        t.setUsed(true);
                        t.setUsedAt(Instant.now());
                        verificationTokenRepository.save(t);
                    });

            String resetToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
            VerificationToken tokenEntity = VerificationToken.builder()
                    .token(resetToken)
                    .userId(user.getId())
                    .email(user.getEmail())
                    .tokenType(TokenType.PASSWORD_RESET)
                    .expiresAt(Instant.now().plus(java.time.Duration.ofHours(1)))
                    .build();
            verificationTokenRepository.save(tokenEntity);

            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
            log.info("Password reset email sent to: {}", request.getEmail());
        });
    }

    public void resetPassword(ResetPasswordRequest request) {
        VerificationToken resetToken = verificationTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (resetToken.isUsed()) {
            throw new BadRequestException("Reset token has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Reset token has expired");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", resetToken.getUserId()));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(Instant.now());
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        userRepository.save(user);

        resetToken.setUsed(true);
        resetToken.setUsedAt(Instant.now());
        verificationTokenRepository.save(resetToken);

        tokenService.revokeAllUserTokens(user.getId());

        auditService.recordPasswordResetEvent(user.getId(), user.getEmail());
        emailService.sendPasswordChangedNotification(user.getEmail());

        log.info("Password reset completed for user: {}", user.getUsername());
    }

    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New passwords do not match");
        }

        if (request.getCurrentPassword().equals(request.getNewPassword())) {
            throw new BadRequestException("New password must be different from current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        tokenService.revokeAllUserTokens(user.getId());
        emailService.sendPasswordChangedNotification(user.getEmail());

        log.info("Password changed for user: {}", user.getUsername());
    }

    private AuthResponse generateAuthResponse(User user, String ipAddress,
                                              String userAgent, String authMethod) {
        String userId = user.getId();
        String username = user.getUsername();
        Set<String> roles = Set.of("ROLE_" + user.getRole().name());

        String accessToken = jwtTokenProvider.generateAccessToken(userId, username, roles);
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(userId, username);

        String deviceId = UUID.randomUUID().toString();
        String deviceName = parseDeviceName(userAgent);

        tokenService.storeRefreshToken(refreshTokenValue, userId, username,
                deviceId, deviceName, ipAddress, userAgent,
                jwtTokenProvider.getRefreshTokenExpirationMs());

        String sessionToken = UUID.randomUUID().toString();
        tokenService.createSession(userId, sessionToken, refreshTokenValue,
                ipAddress, deviceId, deviceName, userAgent,
                Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenExpirationMs()));

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs() / 1000)
                .userId(userId)
                .username(username)
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    private String parseDeviceName(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Mobile")) return "Mobile Device";
        if (userAgent.contains("Windows")) return "Windows PC";
        if (userAgent.contains("Mac")) return "Mac";
        if (userAgent.contains("Linux")) return "Linux Desktop";
        if (userAgent.contains("Postman")) return "Postman";
        return "Web Browser";
    }
}
