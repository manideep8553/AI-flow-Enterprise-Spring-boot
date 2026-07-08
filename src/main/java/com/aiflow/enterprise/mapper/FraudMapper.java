package com.aiflow.enterprise.mapper;

import com.aiflow.enterprise.dto.response.FraudAlertResponse;
import com.aiflow.enterprise.dto.response.FraudCheckResponse;
import com.aiflow.enterprise.dto.response.FraudRuleResponse;
import com.aiflow.enterprise.entity.FraudAlert;
import com.aiflow.enterprise.entity.FraudCheck;
import com.aiflow.enterprise.entity.FraudRule;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FraudMapper {

    FraudCheckResponse toCheckResponse(FraudCheck fraudCheck);

    List<FraudCheckResponse> toCheckResponseList(List<FraudCheck> fraudChecks);

    FraudAlertResponse toAlertResponse(FraudAlert fraudAlert);

    List<FraudAlertResponse> toAlertResponseList(List<FraudAlert> fraudAlerts);

    FraudRuleResponse toRuleResponse(FraudRule fraudRule);

    List<FraudRuleResponse> toRuleResponseList(List<FraudRule> fraudRules);
}
