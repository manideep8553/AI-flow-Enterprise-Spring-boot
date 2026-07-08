package com.aiflow.enterprise.controller.v1;

import com.aiflow.enterprise.dto.request.TriggerRequest;
import com.aiflow.enterprise.dto.response.ApiResponse;
import com.aiflow.enterprise.dto.response.PageResponse;
import com.aiflow.enterprise.dto.response.TriggerResponse;
import com.aiflow.enterprise.service.TriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/triggers")
@Tag(name = "Triggers", description = "Trigger management APIs")
public class TriggerController {

    private final TriggerService triggerService;

    public TriggerController(TriggerService triggerService) {
        this.triggerService = triggerService;
    }

    @PostMapping
    @Operation(summary = "Create a new trigger")
    public ResponseEntity<ApiResponse<TriggerResponse>> createTrigger(@Valid @RequestBody TriggerRequest request) {
        TriggerResponse response = triggerService.createTrigger(request);
        return new ResponseEntity<>(ApiResponse.created(response), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get trigger by ID")
    public ResponseEntity<ApiResponse<TriggerResponse>> getTriggerById(@PathVariable String id) {
        TriggerResponse response = triggerService.getTriggerById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "List triggers with pagination and filtering")
    public ResponseEntity<ApiResponse<PageResponse<TriggerResponse>>> getAllTriggers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String workflowId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean active) {
        Page<TriggerResponse> triggerPage = triggerService.getAllTriggers(
                page, size, workflowId, type, active);
        PageResponse<TriggerResponse> pageResponse = PageResponse.from(
                triggerPage, triggerPage.getContent());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a trigger")
    public ResponseEntity<ApiResponse<TriggerResponse>> updateTrigger(
            @PathVariable String id,
            @Valid @RequestBody TriggerRequest request) {
        TriggerResponse response = triggerService.updateTrigger(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Trigger updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a trigger")
    public ResponseEntity<ApiResponse<Void>> deleteTrigger(@PathVariable String id) {
        triggerService.deleteTrigger(id);
        return ResponseEntity.ok(ApiResponse.deleted());
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate a trigger")
    public ResponseEntity<ApiResponse<TriggerResponse>> activateTrigger(@PathVariable String id) {
        TriggerResponse response = triggerService.activateTrigger(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Trigger activated successfully"));
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a trigger")
    public ResponseEntity<ApiResponse<TriggerResponse>> deactivateTrigger(@PathVariable String id) {
        TriggerResponse response = triggerService.deactivateTrigger(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Trigger deactivated successfully"));
    }
}
