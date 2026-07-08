package com.aiflow.enterprise.dto.response;

import com.aiflow.enterprise.enums.UserRole;
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
public class UserResponse {

    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private String department;
    private Boolean active;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
}
