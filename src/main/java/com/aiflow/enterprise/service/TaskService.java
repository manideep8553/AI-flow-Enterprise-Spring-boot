package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.request.TaskRequest;
import com.aiflow.enterprise.dto.request.TaskStatusRequest;
import com.aiflow.enterprise.dto.response.TaskResponse;
import org.springframework.data.domain.Page;

public interface TaskService {

    TaskResponse createTask(TaskRequest request);

    TaskResponse getTaskById(String id);

    Page<TaskResponse> getAllTasks(int page, int size, String workflowId, String executionId, String assignee, String status);

    TaskResponse updateTask(String id, TaskRequest request);

    TaskResponse updateTaskStatus(String id, TaskStatusRequest request);

    TaskResponse assignTask(String id, String assignee);
}
