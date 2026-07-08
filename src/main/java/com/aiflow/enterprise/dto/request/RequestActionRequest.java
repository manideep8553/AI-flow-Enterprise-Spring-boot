package com.aiflow.enterprise.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestActionRequest {

    @NotBlank
    private String action;

    private String comment;

    private String assignedTo;

    private Map<String, Object> additionalData;
}
