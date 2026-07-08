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
public class AIQueryRequest {

    @NotBlank
    private String query;

    private String contextType;

    private String contextId;

    private String workflowId;

    private String executionId;

    private String requestId;

    private Map<String, Object> additionalContext;
}
