package com.aiflow.enterprise.engine.core;

import com.aiflow.enterprise.engine.ExecutionContext;
import com.aiflow.enterprise.entity.Task;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.enums.StepType;
import com.aiflow.enterprise.enums.TaskPriority;
import com.aiflow.enterprise.enums.TaskStatus;
import com.aiflow.enterprise.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ApprovalStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(ApprovalStepProcessor.class);
    private final TaskRepository taskRepository;

    public ApprovalStepProcessor(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public StepType getType() { return StepType.APPROVAL; }

    @Override
    public StepResult execute(WorkflowStep step, ExecutionContext ctx) {
        Map<String, Object> config = step.getConfig();
        String assignee = getConfig(config, "assignee", "");
        String title = resolveTemplate(getConfig(config, "title", "Approval Required: " + step.getName()), ctx);
        boolean parallel = Boolean.parseBoolean(getConfig(config, "parallel", "false"));
        String strategy = getConfig(config, "strategy", "all");

        List<String> assignees;
        if (parallel && config.get("assignees") instanceof List) {
            assignees = getConfigTyped(config, "assignees", List.class);
        } else if (!assignee.isBlank()) {
            assignees = List.of(assignee.split(","));
        } else {
            assignees = List.of();
        }

        if (assignees.isEmpty()) {
            return StepResult.failure("No assignees configured for approval step: " + step.getName());
        }

        String executionId = ctx.getExecutionId();
        for (String a : assignees) {
            Task task = Task.builder()
                    .workflowId(ctx.getWorkflowId())
                    .executionId(executionId)
                    .workflowStepId(step.getStepId())
                    .name(title)
                    .description(getConfig(config, "description", ""))
                    .assignee(a.trim())
                    .status(TaskStatus.PENDING)
                    .priority(TaskPriority.HIGH)
                    .metadata(Map.of("type", "approval", "strategy", strategy, "parallel", parallel))
                    .build();
            taskRepository.save(task);
        }

        if (Boolean.parseBoolean(getConfig(config, "autoApprove", "false"))) {
            Map<String, Object> data = new HashMap<>();
            data.put("approved", true);
            data.put("status", "APPROVED");
            data.put("strategy", strategy);
            return StepResult.success(data, data);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("status", "PENDING_APPROVAL");
        data.put("assignees", assignees);
        data.put("strategy", strategy);
        data.put("parallel", parallel);
        return StepResult.pendingApproval(data);
    }
}
