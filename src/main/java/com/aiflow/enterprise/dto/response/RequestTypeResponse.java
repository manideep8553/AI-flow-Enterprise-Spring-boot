package com.aiflow.enterprise.dto.response;

import com.aiflow.enterprise.entity.embedded.EscalationRule;
import com.aiflow.enterprise.entity.embedded.FieldDefinition;
import com.aiflow.enterprise.entity.embedded.ValidationRule;
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
public class RequestTypeResponse {

    private String id;
    private String name;
    private String description;
    private String icon;
    private String color;
    private String category;
    private String workflowId;
    private String workflowName;
    private boolean active;
    private List<FieldDefinition> fields;
    private List<ValidationRule> globalValidationRules;
    private List<EscalationRule> escalationRules;
    private List<RequestStatus> allowedStatuses;
    private Map<String, List<RequestStatus>> statusTransitions;
    private Integer defaultPriority;
    private boolean allowAttachments;
    private long maxAttachmentSize;
    private List<String> allowedMimeTypes;
    private boolean requireCommentsOnApprove;
    private boolean requireCommentsOnReject;
    private boolean allowRequesterCancellation;
    private boolean allowWithdrawal;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
}
