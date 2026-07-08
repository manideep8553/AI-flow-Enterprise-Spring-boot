package com.aiflow.enterprise.entity.embedded;

import com.aiflow.enterprise.enums.EscalationAction;
import com.aiflow.enterprise.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalationRule {
    private String ruleName;
    private RequestStatus fromStatus;
    private RequestStatus toStatus;
    private int timeoutHours;
    private EscalationAction action;
    private String escalateTo;
    private String escalateToRole;
    private boolean notifyRequester;
    private boolean notifyAssignee;
    private String messageTemplate;
}
