package com.aiflow.enterprise.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "user_sessions")
@CompoundIndex(def = "{'userId': 1, 'active': 1}")
public class UserSession extends BaseEntity {

    @Indexed
    private String userId;

    @Indexed(unique = true)
    private String sessionToken;

    private String refreshToken;

    @Indexed
    private boolean active;

    private Instant lastActivityAt;

    private Instant expiresAt;

    private Instant loggedOutAt;

    private String ipAddress;

    private String deviceId;

    private String deviceName;

    private String deviceType;

    private String userAgent;

    private String location;

    @Builder.Default
    private int tokenVersion = 1;
}
