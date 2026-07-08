package com.aiflow.enterprise.dto.request;

import com.aiflow.enterprise.enums.UserRole;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Email(message = "Email must be valid")
    private String email;

    private String firstName;

    private String lastName;

    private UserRole role;

    private String department;
}
