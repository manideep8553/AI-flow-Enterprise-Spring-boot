package com.aiflow.enterprise.engine.core;

import com.aiflow.enterprise.engine.ExecutionContext;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.enums.StepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConditionStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(ConditionStepProcessor.class);

    @Override
    public StepType getType() { return StepType.CONDITION; }

    @Override
    public StepResult execute(WorkflowStep step, ExecutionContext ctx) {
        Map<String, Object> config = step.getConfig();
        String field = getConfig(config, "field", "");
        String operator = getConfig(config, "operator", "equals");
        String expectedValue = resolveTemplate(getConfig(config, "value", ""), ctx);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> branches = (List<Map<String, Object>>) config.get("branches");

        Object actualValue = null;
        if (!field.isBlank()) {
            actualValue = resolveFromContext(field, ctx);
        }

        boolean matched = evaluateCondition(actualValue, operator, expectedValue);

        String matchedBranch = null;
        String nextStepId = null;

        if (branches != null) {
            for (Map<String, Object> branch : branches) {
                String branchCondition = (String) branch.get("condition");
                if (branchCondition == null
                        || (matched && "true".equals(branchCondition))
                        || (!matched && "false".equals(branchCondition))
                        || (branchCondition != null && branchCondition.equals(field + "_" + operator + "_" + expectedValue))) {
                    matchedBranch = (String) branch.get("name");
                    nextStepId = (String) branch.get("nextStepId");
                    break;
                }
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("matched", matched);
        data.put("field", field);
        data.put("operator", operator);
        data.put("expectedValue", expectedValue);
        data.put("actualValue", actualValue);
        data.put("branch", matchedBranch);
        if (nextStepId != null) data.put("nextStepId", nextStepId);

        log.info("Condition: field={} {} {} -> matched={}, branch={}",
                field, operator, expectedValue, matched, matchedBranch);
        return StepResult.success(data, data);
    }

    private boolean evaluateCondition(Object actual, String operator, String expected) {
        String actualStr = actual != null ? actual.toString() : "";
        return switch (operator.toLowerCase()) {
            case "equals", "==" -> actualStr.equals(expected);
            case "not_equals", "!=" -> !actualStr.equals(expected);
            case "contains" -> actualStr.contains(expected);
            case "not_contains" -> !actualStr.contains(expected);
            case "starts_with" -> actualStr.startsWith(expected);
            case "ends_with" -> actualStr.endsWith(expected);
            case "gt", ">" -> compareNumeric(actualStr, expected) > 0;
            case "gte", ">=" -> compareNumeric(actualStr, expected) >= 0;
            case "lt", "<" -> compareNumeric(actualStr, expected) < 0;
            case "lte", "<=" -> compareNumeric(actualStr, expected) <= 0;
            case "is_empty" -> actualStr.isBlank();
            case "is_not_empty" -> !actualStr.isBlank();
            case "regex" -> actualStr.matches(expected);
            case "in" -> expected.contains(actualStr);
            case "not_in" -> !expected.contains(actualStr);
            default -> actualStr.equalsIgnoreCase(expected);
        };
    }

    private double compareNumeric(String a, String b) {
        try { return Double.parseDouble(a) - Double.parseDouble(b); }
        catch (NumberFormatException e) { return a.compareTo(b); }
    }
}
