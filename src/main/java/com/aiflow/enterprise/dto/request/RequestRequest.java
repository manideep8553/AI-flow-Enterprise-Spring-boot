package com.aiflow.enterprise.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestRequest {

    @NotBlank
    private String requestTypeId;

    @NotBlank
    private String title;

    private String description;

    private Instant dueDate;

    private Integer priority;

    private Map<String, Object> fields;

    private List<String> attachmentIds;

    private String assignedTo;
}
