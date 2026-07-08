package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.request.TaskRequest;
import com.aiflow.enterprise.dto.request.TaskStatusRequest;
import com.aiflow.enterprise.dto.response.TaskResponse;
import com.aiflow.enterprise.entity.Task;
import com.aiflow.enterprise.enums.TaskStatus;
import com.aiflow.enterprise.exception.BadRequestException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.mapper.TaskMapper;
import com.aiflow.enterprise.repository.TaskRepository;
import com.aiflow.enterprise.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskServiceImpl(TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    public TaskResponse createTask(TaskRequest request) {
        Task task = taskMapper.toEntity(request);
        if (task.getPriority() == null) {
            task.setPriority(com.aiflow.enterprise.enums.TaskPriority.MEDIUM);
        }
        Task saved = taskRepository.save(task);
        log.info("Task created: {} for execution {}", saved.getName(), saved.getExecutionId());
        return taskMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(String id) {
        Task task = findTaskOrThrow(id);
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TaskResponse> getAllTasks(int page, int size, String workflowId,
                                          String executionId, String assignee, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Task> taskPage;

        if (workflowId != null && status != null) {
            TaskStatus taskStatus = TaskStatus.valueOf(status.toUpperCase());
            taskPage = taskRepository.findByWorkflowIdAndStatus(workflowId, taskStatus, pageable);
        } else if (workflowId != null) {
            taskPage = taskRepository.findByWorkflowId(workflowId, pageable);
        } else if (executionId != null) {
            taskPage = taskRepository.findByExecutionId(executionId, pageable);
        } else if (assignee != null) {
            taskPage = taskRepository.findByAssignee(assignee, pageable);
        } else if (status != null) {
            TaskStatus taskStatus = TaskStatus.valueOf(status.toUpperCase());
            taskPage = taskRepository.findByStatus(taskStatus, pageable);
        } else {
            taskPage = taskRepository.findAll(pageable);
        }

        return taskPage.map(taskMapper::toResponse);
    }

    @Override
    public TaskResponse updateTask(String id, TaskRequest request) {
        Task existing = findTaskOrThrow(id);
        taskMapper.updateEntity(request, existing);
        Task saved = taskRepository.save(existing);
        log.info("Task updated: {}", id);
        return taskMapper.toResponse(saved);
    }

    @Override
    public TaskResponse updateTaskStatus(String id, TaskStatusRequest request) {
        Task task = findTaskOrThrow(id);
        TaskStatus newStatus = request.getStatus();
        TaskStatus currentStatus = task.getStatus();

        if (currentStatus == TaskStatus.COMPLETED || currentStatus == TaskStatus.FAILED
                || currentStatus == TaskStatus.SKIPPED) {
            throw new BadRequestException(
                    "Cannot update status of a task in terminal state: " + currentStatus);
        }

        task.setStatus(newStatus);
        if (request.getResult() != null) {
            task.setResult(request.getResult());
        }
        if (request.getComments() != null) {
            task.setComments(request.getComments());
        }
        if (newStatus == TaskStatus.COMPLETED || newStatus == TaskStatus.FAILED) {
            task.setCompletedAt(Instant.now());
        }
        Task saved = taskRepository.save(task);
        log.info("Task {} status updated from {} to {}", id, currentStatus, newStatus);
        return taskMapper.toResponse(saved);
    }

    @Override
    public TaskResponse assignTask(String id, String assignee) {
        Task task = findTaskOrThrow(id);
        task.setAssignee(assignee);
        if (task.getStatus() == TaskStatus.PENDING) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }
        Task saved = taskRepository.save(task);
        log.info("Task {} assigned to {}", id, assignee);
        return taskMapper.toResponse(saved);
    }

    private Task findTaskOrThrow(String id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", "id", id));
    }
}
