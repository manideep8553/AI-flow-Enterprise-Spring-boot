package com.aiflow.enterprise.entity;

import com.aiflow.enterprise.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "users")
public class User extends BaseEntity {

    @Indexed(unique = true)
    private String username;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;

    private String firstName;

    private String lastName;

    @Indexed
    private UserRole role;

    @Indexed
    private String department;

    @Indexed
    private Boolean active;

    private Instant lastLoginAt;

    private String lastLoginIp;

    private String lastLoginDevice;

    private String lastLoginLocation;

    /* --- Account security fields --- */
    @Builder.Default
    private int failedLoginAttempts = 0;

    private Instant lockedUntil;

    @Builder.Default
    private boolean accountLocked = false;

    @Builder.Default
    private boolean emailVerified = false;

    private Instant emailVerifiedAt;

    private String emailVerificationToken;

    private Instant emailVerificationTokenExpiry;

    private String passwordResetToken;

    private Instant passwordResetTokenExpiry;

    private Instant passwordChangedAt;

    @Builder.Default
    private boolean mfaEnabled = false;

    private String mfaSecret;

    @Builder.Default
    private Set<String> authorities = Set.of();

    private String authProvider;

    private String authProviderId;

    private String avatarUrl;

    private String refreshToken;
}
