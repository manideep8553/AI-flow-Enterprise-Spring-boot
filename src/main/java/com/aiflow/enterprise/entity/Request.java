package com.aiflow.enterprise.entity;

import com.aiflow.enterprise.entity.embedded.ApprovalEntry;
import com.aiflow.enterprise.entity.embedded.CommentEntry;
import com.aiflow.enterprise.entity.embedded.FileAttachment;
import com.aiflow.enterprise.entity.embedded.StatusChangeEntry;
import com.aiflow.enterprise.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "requests")
public class Request extends BaseEntity {

    @Indexed
    private String requestTypeId;

    private String requestTypeName;

    @Indexed
    private String title;

    private String description;

    @Indexed
    private RequestStatus status;

    @Indexed
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

    @Indexed
    private String currentApprover;

    private String currentApproverName;

    @Indexed
    private String workflowExecutionId;

    private String workflowId;

    private String workflowName;

    @Indexed
    private String assignedTo;

    private String assignedToName;

    private String departmentId;

    private String departmentName;

    private boolean escalated;

    private Instant escalatedAt;

    private String escalationReason;

    @Builder.Default
    private int version = 1;
}
