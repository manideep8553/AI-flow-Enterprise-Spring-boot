package com.aiflow.enterprise.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestFilterRequest {

    private String requestTypeId;

    private List<String> statuses;

    private String submittedBy;

    private String assignedTo;

    private String departmentId;

    private String priority;

    private Instant submittedAfter;

    private Instant submittedBefore;

    private Instant dueBefore;

    private String searchText;

    private boolean escalated;
}
