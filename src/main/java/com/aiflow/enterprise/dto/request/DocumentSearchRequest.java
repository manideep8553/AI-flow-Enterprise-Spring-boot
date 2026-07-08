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
public class DocumentSearchRequest {
    private String searchText;
    private String documentType;
    private String processingStatus;
    private String uploadedBy;
    private String tag;
    private String category;
    private Boolean archived;
}
