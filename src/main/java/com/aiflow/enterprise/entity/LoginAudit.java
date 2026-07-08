package com.aiflow.enterprise.entity;

import lombok.AllArgsConstructor;
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
@Document(collection = "login_audits")
@CompoundIndex(def = "{'userId': 1, 'timestamp': -1}")
@CompoundIndex(def = "{'email': 1, 'timestamp': -1}")
public class LoginAudit extends BaseEntity {

    @Indexed
    private String userId;

    @Indexed
    private String email;

    private String username;

    @Indexed
    private String action;

    @Indexed
    private boolean success;

    private String failureReason;

    private String ipAddress;

    private String deviceId;

    private String deviceName;

    private String userAgent;

    private String location;

    @Indexed
    private Instant timestamp;

    private String authMethod;

    private String sessionId;
}
