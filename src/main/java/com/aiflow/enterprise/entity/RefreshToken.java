package com.aiflow.enterprise.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @Indexed(unique = true)
    private String token;

    @Indexed
    private String userId;

    @Indexed
    private String username;

    private String deviceId;

    private String deviceName;

    private String ipAddress;

    @Indexed
    private Instant expiresAt;

    @Builder.Default
    private boolean revoked = false;

    private Instant revokedAt;

    private String replacedByToken;

    private String previousToken;

    private String userAgent;
}
