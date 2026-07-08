package com.aiflow.enterprise.engine.core;

import com.aiflow.enterprise.engine.ExecutionContext;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.enums.StepType;

import java.util.Map;

public interface StepProcessor {

    StepType getType();

    StepResult execute(WorkflowStep step, ExecutionContext ctx);

    default String resolveTemplate(String template, ExecutionContext ctx) {
        if (template == null) return "";
        String result = template;
        for (var entry : ctx.getVariables().entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}",
                    entry.getValue() != null ? entry.getValue().toString() : "");
        }
        return result;
    }

    default Object resolveFromContext(String field, ExecutionContext ctx) {
        if (field.startsWith("{{") && field.endsWith("}}")) {
            String key = field.substring(2, field.length() - 2).trim();
            return ctx.getVariable(key) != null ? ctx.getVariable(key) : field;
        }
        return ctx.getVariable(field) != null ? ctx.getVariable(field) : field;
    }

    default String getConfig(Map<String, Object> config, String key, String defaultValue) {
        Object val = config != null ? config.get(key) : null;
        return val != null ? val.toString() : defaultValue;
    }

    @SuppressWarnings("unchecked")
    default <T> T getConfigTyped(Map<String, Object> config, String key, Class<T> type) {
        Object val = config != null ? config.get(key) : null;
        return val != null && type.isInstance(val) ? (T) val : null;
    }
}
