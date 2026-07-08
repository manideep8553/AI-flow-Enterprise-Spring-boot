package com.aiflow.enterprise.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSessionResponse {

    private String id;
    private String userId;
    private boolean active;
    private Instant lastActivityAt;
    private Instant expiresAt;
    private Instant createdAt;
    private String ipAddress;
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String userAgent;
    private String location;
    private boolean currentSession;
}
