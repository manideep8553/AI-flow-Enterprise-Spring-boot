package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.request.TaskAssignRequest;
import com.aiflow.enterprise.dto.request.TaskRequest;
import com.aiflow.enterprise.dto.request.TaskStatusRequest;
import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.PageResponse;
import com.aiflow.enterprise.dto.response.TaskResponse;
import com.aiflow.enterprise.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks", description = "Task management APIs")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(@Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.createTask(request);
        return new ResponseEntity<>(ApiResponse.created(response), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(@PathVariable String id) {
        TaskResponse response = taskService.getTaskById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List tasks with pagination and filtering")
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> getAllTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String workflowId,
            @RequestParam(required = false) String executionId,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String status) {
        Page<TaskResponse> taskPage = taskService.getAllTasks(
                page, size, workflowId, executionId, assignee, status);
        PageResponse<TaskResponse> pageResponse = PageResponse.from(taskPage, taskPage.getContent());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable String id,
            @Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.updateTask(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Task updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update task status")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskStatus(
            @PathVariable String id,
            @Valid @RequestBody TaskStatusRequest request) {
        TaskResponse response = taskService.updateTaskStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Task status updated"));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assign task to a user")
    public ResponseEntity<ApiResponse<TaskResponse>> assignTask(
            @PathVariable String id,
            @Valid @RequestBody TaskAssignRequest request) {
        TaskResponse response = taskService.assignTask(id, request.getAssignee());
        return ResponseEntity.ok(ApiResponse.success(response, "Task assigned successfully"));
    }
}
