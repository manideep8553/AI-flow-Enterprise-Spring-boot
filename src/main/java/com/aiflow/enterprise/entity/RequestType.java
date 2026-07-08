package com.aiflow.enterprise.entity;

import com.aiflow.enterprise.entity.embedded.EscalationRule;
import com.aiflow.enterprise.entity.embedded.FieldDefinition;
import com.aiflow.enterprise.entity.embedded.ValidationRule;
import com.aiflow.enterprise.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "request_types")
public class RequestType extends BaseEntity {

    @Indexed(unique = true)
    private String name;

    private String description;

    private String icon;

    private String color;

    private String category;

    @Indexed
    private String workflowId;

    private String workflowName;

    @Builder.Default
    private boolean active = true;

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
}
