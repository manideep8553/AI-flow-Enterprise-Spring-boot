package com.aiflow.enterprise.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudReviewRequest {

    @NotBlank
    private String fraudCheckId;

    @NotBlank
    private String status;

    private String reviewNotes;

    private String assignedTo;
}
