package com.aiflow.enterprise.service;

import com.aiflow.enterprise.entity.User;
import com.aiflow.enterprise.exception.BadRequestException;
import com.aiflow.enterprise.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@Transactional
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);

    private final UserRepository userRepository;
    private final int maxFailedAttempts;
    private final long lockDurationMinutes;

    public LoginAttemptService(UserRepository userRepository,
                               @Value("${app.auth.max-failed-login-attempts:5}") int maxFailedAttempts,
                               @Value("${app.auth.lock-duration-minutes:30}") long lockDurationMinutes) {
        this.userRepository = userRepository;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    public void recordFailedAttempt(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= maxFailedAttempts) {
            Instant lockUntil = Instant.now().plus(Duration.ofMinutes(lockDurationMinutes));
            user.setLockedUntil(lockUntil);
            user.setAccountLocked(true);
            log.warn("Account locked for user {} until {} due to {} failed attempts",
                    user.getUsername(), lockUntil, attempts);
        }

        userRepository.save(user);
    }

    public void resetFailedAttempts(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    public void checkIfAccountLocked(User user) {
        if (user.isAccountLocked()) {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
                long minutesRemaining = Duration.between(Instant.now(), user.getLockedUntil()).toMinutes();
                throw new BadRequestException(
                        String.format("Account is locked due to too many failed login attempts. " +
                                "Please try again in %d minutes.", minutesRemaining + 1));
            } else {
                user.setAccountLocked(false);
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        }
    }

    public boolean isAccountLocked(User user) {
        if (!user.isAccountLocked()) return false;
        if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(Instant.now())) {
            user.setAccountLocked(false);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
            return false;
        }
        return true;
    }
}
