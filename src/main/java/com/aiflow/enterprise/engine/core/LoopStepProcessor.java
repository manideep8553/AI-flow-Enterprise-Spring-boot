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
public class LoopStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(LoopStepProcessor.class);

    @Override
    public StepType getType() { return StepType.LOOP; }

    @Override
    @SuppressWarnings("unchecked")
    public StepResult execute(WorkflowStep step, ExecutionContext ctx) {
        Map<String, Object> config = step.getConfig();
        String loopType = getConfig(config, "loopType", "forEach");
        String collectionExpression = getConfig(config, "collection", "");
        String conditionExpression = getConfig(config, "condition", "");
        String loopVariable = getConfig(config, "loopVariable", "item");
        int maxIterations = Integer.parseInt(getConfig(config, "maxIterations", "1000"));
        int currentIteration = ctx.getLoopCount(step.getStepId());

        if (currentIteration >= maxIterations) {
            log.warn("Loop max iterations reached for step: {}", step.getStepId());
            return StepResult.success(Map.of("completed", true, "iterations", currentIteration, "maxReached", true));
        }

        switch (loopType) {
            case "forEach" -> {
                Object collectionObj = resolveFromContext(collectionExpression, ctx);
                List<Object> collection;
                if (collectionObj instanceof List) {
                    collection = (List<Object>) collectionObj;
                } else if (collectionObj instanceof Object[]) {
                    collection = List.of((Object[]) collectionObj);
                } else {
                    return StepResult.failure("Collection not found or not iterable: " + collectionExpression);
                }

                if (currentIteration >= collection.size()) {
                    return StepResult.success(Map.of("completed", true, "iterations", currentIteration));
                }

                Object item = collection.get(currentIteration);
                ctx.setVariable(loopVariable, item);
                ctx.setVariable(loopVariable + "_index", currentIteration);
                ctx.incrementLoop(step.getStepId());

                Map<String, Object> data = new HashMap<>();
                data.put("continue", true);
                data.put("index", currentIteration);
                data.put("total", collection.size());
                data.put("item", item);
                data.put("loopVariable", loopVariable);
                return StepResult.success(data, data);
            }
            case "repeat" -> {
                ctx.incrementLoop(step.getStepId());
                int count = currentIteration + 1;
                ctx.setVariable(loopVariable, count);
                ctx.setVariable(loopVariable + "_index", count);

                boolean shouldContinue = true;
                if (!conditionExpression.isBlank()) {
                    Object conditionVal = resolveFromContext(conditionExpression, ctx);
                    shouldContinue = Boolean.parseBoolean(conditionVal.toString());
                }

                Map<String, Object> data = new HashMap<>();
                data.put("continue", shouldContinue);
                data.put("iteration", count);
                return StepResult.success(data, data);
            }
            case "while" -> {
                Object conditionVal = resolveFromContext(conditionExpression, ctx);
                boolean shouldContinue = Boolean.parseBoolean(conditionVal.toString());

                if (!shouldContinue) {
                    return StepResult.success(Map.of("completed", true, "iterations", currentIteration));
                }

                ctx.incrementLoop(step.getStepId());
                ctx.setVariable(loopVariable + "_index", currentIteration);

                Map<String, Object> data = new HashMap<>();
                data.put("continue", true);
                data.put("iteration", currentIteration);
                return StepResult.success(data, data);
            }
            default -> {
                ctx.incrementLoop(step.getStepId());
                return StepResult.success(Map.of("continue", true, "iteration", currentIteration + 1));
            }
        }
    }
}
