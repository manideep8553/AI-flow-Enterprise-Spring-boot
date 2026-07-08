package com.aiflow.enterprise.engine.core;

import com.aiflow.enterprise.engine.ExecutionContext;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.enums.StepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class TransformationStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(TransformationStepProcessor.class);

    @Override
    public StepType getType() { return StepType.TRANSFORMATION; }

    @Override
    @SuppressWarnings("unchecked")
    public StepResult execute(WorkflowStep step, ExecutionContext ctx) {
        Map<String, Object> config = step.getConfig();
        String expression = getConfig(config, "expression", "");
        String targetVariable = getConfig(config, "targetVariable", "");
        Map<String, Object> mappings = (Map<String, Object>) config.get("mappings");

        Map<String, Object> result = new HashMap<>();

        if (mappings != null) {
            for (Map.Entry<String, Object> entry : mappings.entrySet()) {
                String targetKey = entry.getKey();
                String sourceExpression = entry.getValue().toString();
                Object resolved = resolveFromContext(sourceExpression, ctx);
                result.put(targetKey, resolved);
                if (!targetVariable.isBlank()) {
                    ctx.setVariable(targetVariable, result);
                }
            }
        }

        if (!expression.isBlank()) {
            log.info("Transformation: {}", expression);
            result.put("transformed", true);
            result.put("expression", expression);
        }

        if (!targetVariable.isBlank() && !result.isEmpty()) {
            ctx.setVariable(targetVariable, result);
        }

        return StepResult.success(result, result);
    }
}
