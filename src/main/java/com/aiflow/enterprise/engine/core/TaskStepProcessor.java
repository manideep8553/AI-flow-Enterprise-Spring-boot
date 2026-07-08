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

import java.util.Map;

@Component
public class TaskStepProcessor implements StepProcessor {

    private static final Logger log = LoggerFactory.getLogger(TaskStepProcessor.class);

    private final TaskRepository taskRepository;

    public TaskStepProcessor(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public StepType getType() { return StepType.TASK; }

    @Override
    public StepResult execute(WorkflowStep step, ExecutionContext ctx) {
        Map<String, Object> config = step.getConfig();
        String taskName = resolveTemplate(getConfig(config, "taskName", step.getName()), ctx);
        String assignee = resolveTemplate(getConfig(config, "assignee", ""), ctx);
        String priorityStr = getConfig(config, "priority", "MEDIUM");

        TaskPriority priority;
        try { priority = TaskPriority.valueOf(priorityStr.toUpperCase()); }
        catch (IllegalArgumentException e) { priority = TaskPriority.MEDIUM; }

        Task task = Task.builder()
                .workflowId(ctx.getWorkflowId())
                .executionId(ctx.getExecutionId())
                .workflowStepId(step.getStepId())
                .name(taskName)
                .description(getConfig(config, "description", ""))
                .assignee(assignee)
                .status(TaskStatus.PENDING)
                .priority(priority)
                .dueDate(null)
                .metadata(config)
                .build();
        taskRepository.save(task);

        log.info("Task created: {} assignee={} priority={}", taskName, assignee, priority);
        Map<String, Object> data = Map.of(
            "executed", true, "taskName", taskName,
            "assignee", assignee, "taskId", task.getId()
        );
        return StepResult.success(data, data);
    }
}
