package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.request.RequestTypeRequest;
import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.PageResponse;
import com.aiflow.enterprise.dto.response.RequestTypeResponse;
import com.aiflow.enterprise.service.RequestTypeService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/request-types")
@Tag(name = "Request Types", description = "Configurable request type management APIs")
public class RequestTypeController {

    private final RequestTypeService requestTypeService;

    public RequestTypeController(RequestTypeService requestTypeService) {
        this.requestTypeService = requestTypeService;
    }

    @PostMapping
    @Operation(summary = "Create a new request type")
    public ResponseEntity<ApiResponse<RequestTypeResponse>> createRequestType(
            @Valid @RequestBody RequestTypeRequest request) {
        RequestTypeResponse response = requestTypeService.createRequestType(request, "admin");
        return ResponseEntity.ok(ApiResponse.success(response, "Request type created"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a request type")
    public ResponseEntity<ApiResponse<RequestTypeResponse>> updateRequestType(
            @PathVariable String id, @Valid @RequestBody RequestTypeRequest request) {
        RequestTypeResponse response = requestTypeService.updateRequestType(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Request type updated"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get request type by ID")
    public ResponseEntity<ApiResponse<RequestTypeResponse>> getRequestTypeById(@PathVariable String id) {
        RequestTypeResponse response = requestTypeService.getRequestTypeById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List request types with pagination and filtering")
    public ResponseEntity<ApiResponse<PageResponse<RequestTypeResponse>>> getAllRequestTypes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active) {
        Page<RequestTypeResponse> typePage = requestTypeService.getAllRequestTypes(page, size, category, active);
        PageResponse<RequestTypeResponse> pageResponse = PageResponse.from(typePage, typePage.getContent());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @GetMapping("/active")
    @Operation(summary = "Get all active request types")
    public ResponseEntity<ApiResponse<List<RequestTypeResponse>>> getActiveRequestTypes() {
        List<RequestTypeResponse> types = requestTypeService.getActiveRequestTypes();
        return ResponseEntity.ok(ApiResponse.success(types));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a request type")
    public ResponseEntity<ApiResponse<Void>> deleteRequestType(@PathVariable String id) {
        requestTypeService.deleteRequestType(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Request type deleted"));
    }

    @PostMapping("/{id}/toggle-active")
    @Operation(summary = "Toggle active status of a request type")
    public ResponseEntity<ApiResponse<RequestTypeResponse>> toggleActive(@PathVariable String id) {
        RequestTypeResponse response = requestTypeService.toggleActive(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Request type active status toggled"));
    }
}
