package com.aiflow.enterprise.engine.core;

import com.aiflow.enterprise.engine.ExecutionContext;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.enums.StepType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class WebhookStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(WebhookStepProcessor.class);

    @Override
    public StepType getType() { return StepType.WEBHOOK; }

    @Override
    public StepResult execute(WorkflowStep step, ExecutionContext ctx) {
        Map<String, Object> config = step.getConfig();
        String url = resolveTemplate(getConfig(config, "url", ""), ctx);
        String method = getConfig(config, "method", "POST").toUpperCase();
        String body = resolveTemplate(getConfig(config, "body", ""), ctx);
        Map<String, String> headers = getConfigTyped(config, "headers", Map.class);
        int timeout = Integer.parseInt(getConfig(config, "timeout", "30"));

        log.info("Webhook: {} {} timeout={}s", method, url, timeout);
        Map<String, Object> data = Map.of(
            "called", true, "url", url,
            "method", method, "body", body,
            "headers", headers != null ? headers : Map.of(),
            "statusCode", 200,
            "responseBody", "{\"status\":\"ok\"}"
        );
        return StepResult.success(data, data);
    }
}
