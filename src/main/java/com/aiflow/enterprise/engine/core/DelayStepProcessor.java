package com.aiflow.enterprise.engine.core;

import com.aiflow.enterprise.engine.ExecutionContext;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.enums.StepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class DelayStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(DelayStepProcessor.class);

    @Override
    public StepType getType() { return StepType.DELAY; }

    @Override
    public StepResult execute(WorkflowStep step, ExecutionContext ctx) {
        Map<String, Object> config = step.getConfig();
        long seconds = Long.parseLong(getConfig(config, "seconds", "5"));
        String untilExpression = getConfig(config, "until", "");

        if (!untilExpression.isBlank()) {
            Object untilVal = resolveFromContext(untilExpression, ctx);
            if (untilVal instanceof Instant until) {
                seconds = until.getEpochSecond() - Instant.now().getEpochSecond();
                if (seconds < 0) seconds = 0;
            }
        }

        log.info("Delay: {} seconds", seconds);
        Map<String, Object> data = Map.of("delayed", true, "seconds", seconds, "scheduledAt", Instant.now().toString());
        return StepResult.success(data, data);
    }

    public long getDelaySeconds(WorkflowStep step, ExecutionContext ctx) {
        Map<String, Object> config = step.getConfig();
        return Long.parseLong(getConfig(config, "seconds", "5"));
    }
}
