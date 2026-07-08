package com.aiflow.enterprise.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {
    private String category;
    private List<String> tags;
    private String notes;
    private String requestId;
    private String requestTypeId;
}
