package com.aiflow.enterprise.engine.core;

import com.aiflow.enterprise.engine.ExecutionContext;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.enums.StepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NotificationStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationStepProcessor.class);

    @Override
    public StepType getType() { return StepType.NOTIFICATION; }

    @Override
    public StepResult execute(WorkflowStep step, ExecutionContext ctx) {
        Map<String, Object> config = step.getConfig();
        String message = resolveTemplate(getConfig(config, "message", ""), ctx);
        String channel = getConfig(config, "channel", "in-app");
        String subject = resolveTemplate(getConfig(config, "subject", ""), ctx);
        List<String> recipients = getConfigTyped(config, "recipients", List.class);

        log.info("Notification: channel={}, subject={}, message={}, recipients={}",
                channel, subject, message, recipients);

        Map<String, Object> data = Map.of(
            "sent", true,
            "channel", channel,
            "subject", subject,
            "message", message,
            "recipients", recipients != null ? recipients : List.of()
        );
        return StepResult.success(data, data);
    }
}
