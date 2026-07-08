package com.aiflow.enterprise.entity;

import com.aiflow.enterprise.enums.TokenType;
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
@Document(collection = "verification_tokens")
public class VerificationToken extends BaseEntity {

    @Indexed(unique = true)
    private String token;

    @Indexed
    private String userId;

    @Indexed
    private String email;

    private TokenType tokenType;

    @Indexed
    private Instant expiresAt;

    @Builder.Default
    private boolean used = false;

    private Instant usedAt;

    private String ipAddress;

    private String userAgent;
}
