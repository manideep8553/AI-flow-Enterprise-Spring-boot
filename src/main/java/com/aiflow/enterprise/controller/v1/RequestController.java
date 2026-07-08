package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.request.RequestActionRequest;
import com.aiflow.enterprise.dto.request.RequestCommentRequest;
import com.aiflow.enterprise.dto.request.RequestRequest;
import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.PageResponse;
import com.aiflow.enterprise.dto.response.RequestResponse;
import com.aiflow.enterprise.service.RequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/requests")
@Tag(name = "Requests", description = "Request management APIs")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @Operation(summary = "Create a new request")
    public ResponseEntity<ApiResponse<RequestResponse>> createRequest(
            @Valid @RequestBody RequestRequest request) {
        RequestResponse response = requestService.createRequest(request, "current-user");
        return ResponseEntity.ok(ApiResponse.success(response, "Request created"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a draft request")
    public ResponseEntity<ApiResponse<RequestResponse>> updateRequest(
            @PathVariable String id, @Valid @RequestBody RequestRequest request) {
        RequestResponse response = requestService.updateRequest(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Request updated"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get request by ID")
    public ResponseEntity<ApiResponse<RequestResponse>> getRequestById(@PathVariable String id) {
        RequestResponse response = requestService.getRequestById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my requests")
    public ResponseEntity<ApiResponse<PageResponse<RequestResponse>>> getMyRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String requestTypeId) {
        Page<RequestResponse> requestPage = requestService.getMyRequests(
                "current-user", page, size, status, requestTypeId);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(requestPage, requestPage.getContent())));
    }

    @GetMapping("/approvals")
    @Operation(summary = "Get requests pending my approval")
    public ResponseEntity<ApiResponse<PageResponse<RequestResponse>>> getRequestsForApproval(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Page<RequestResponse> requestPage = requestService.getRequestsForApproval(
                "current-user", page, size, status);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(requestPage, requestPage.getContent())));
    }

    @GetMapping
    @Operation(summary = "List all requests with filtering")
    public ResponseEntity<ApiResponse<PageResponse<RequestResponse>>> getAllRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String requestTypeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String submittedBy,
            @RequestParam(required = false) String assignedTo,
            @RequestParam(required = false) Boolean escalated) {
        Page<RequestResponse> requestPage = requestService.getAllRequests(
                page, size, requestTypeId, status, submittedBy, assignedTo, escalated);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(requestPage, requestPage.getContent())));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit a draft request for approval")
    public ResponseEntity<ApiResponse<RequestResponse>> submitRequest(@PathVariable String id) {
        RequestResponse response = requestService.submitRequest(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Request submitted"));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a pending request")
    public ResponseEntity<ApiResponse<RequestResponse>> approveRequest(
            @PathVariable String id, @RequestBody RequestActionRequest action) {
        RequestResponse response = requestService.approveRequest(id, action, "current-user");
        return ResponseEntity.ok(ApiResponse.success(response, "Request approved"));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a pending request")
    public ResponseEntity<ApiResponse<RequestResponse>> rejectRequest(
            @PathVariable String id, @RequestBody RequestActionRequest action) {
        RequestResponse response = requestService.rejectRequest(id, action, "current-user");
        return ResponseEntity.ok(ApiResponse.success(response, "Request rejected"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a request")
    public ResponseEntity<ApiResponse<RequestResponse>> cancelRequest(@PathVariable String id) {
        RequestResponse response = requestService.cancelRequest(id, "current-user");
        return ResponseEntity.ok(ApiResponse.success(response, "Request cancelled"));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to a request")
    public ResponseEntity<ApiResponse<RequestResponse>> addComment(
            @PathVariable String id, @Valid @RequestBody RequestCommentRequest comment) {
        RequestResponse response = requestService.addComment(id, comment, "current-user", "Current User");
        return ResponseEntity.ok(ApiResponse.success(response, "Comment added"));
    }

    @PutMapping("/{id}/fields")
    @Operation(summary = "Update custom fields of a request")
    public ResponseEntity<ApiResponse<RequestResponse>> updateFields(
            @PathVariable String id, @RequestBody Map<String, Object> fields) {
        RequestResponse response = requestService.updateFields(id, fields);
        return ResponseEntity.ok(ApiResponse.success(response, "Fields updated"));
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign a request to a user")
    public ResponseEntity<ApiResponse<RequestResponse>> assignRequest(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        RequestResponse response = requestService.assignRequest(id, body.get("assignedTo"));
        return ResponseEntity.ok(ApiResponse.success(response, "Request assigned"));
    }

    @PostMapping("/{id}/escalate")
    @Operation(summary = "Escalate a request")
    public ResponseEntity<ApiResponse<RequestResponse>> escalateRequest(
            @PathVariable String id, @RequestBody Map<String, String> body) {
        RequestResponse response = requestService.escalateRequest(id, body.get("reason"));
        return ResponseEntity.ok(ApiResponse.success(response, "Request escalated"));
    }

    @GetMapping("/count/type/{requestTypeId}")
    @Operation(summary = "Count requests by type")
    public ResponseEntity<ApiResponse<Long>> countByType(@PathVariable String requestTypeId) {
        long count = requestService.getRequestCountByType(requestTypeId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/count/status/{status}")
    @Operation(summary = "Count requests by status")
    public ResponseEntity<ApiResponse<Long>> countByStatus(@PathVariable String status) {
        long count = requestService.getRequestCountByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a request")
    public ResponseEntity<ApiResponse<Void>> deleteRequest(@PathVariable String id) {
        requestService.deleteRequest(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Request deleted"));
    }
}
