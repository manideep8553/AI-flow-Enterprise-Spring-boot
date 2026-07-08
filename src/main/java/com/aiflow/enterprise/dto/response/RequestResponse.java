package com.aiflow.enterprise.dto.response;

import com.aiflow.enterprise.entity.embedded.ApprovalEntry;
import com.aiflow.enterprise.entity.embedded.CommentEntry;
import com.aiflow.enterprise.entity.embedded.FileAttachment;
import com.aiflow.enterprise.entity.embedded.StatusChangeEntry;
import com.aiflow.enterprise.enums.RequestStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestResponse {

    private String id;
    private String requestTypeId;
    private String requestTypeName;
    private String title;
    private String description;
    private RequestStatus status;
    private String submittedBy;
    private String submittedByName;
    private Instant submittedAt;
    private Instant completedAt;
    private Instant dueDate;
    private Integer priority;
    private Map<String, Object> fields;
    private List<FileAttachment> attachments;
    private List<CommentEntry> comments;
    private List<ApprovalEntry> approvalHistory;
    private List<StatusChangeEntry> statusHistory;
    private String currentApprover;
    private String currentApproverName;
    private String workflowExecutionId;
    private String workflowId;
    private String workflowName;
    private String assignedTo;
    private String assignedToName;
    private String departmentId;
    private String departmentName;
    private boolean escalated;
    private Instant escalatedAt;
    private String escalationReason;
    private int version;
    private Instant createdAt;
    private Instant updatedAt;
}
