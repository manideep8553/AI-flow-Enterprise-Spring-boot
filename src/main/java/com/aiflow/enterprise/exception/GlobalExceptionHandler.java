package com.aiflow.enterprise.exception;

import com.aiflow.enterprise.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        Map<String, Object> errors = new HashMap<>();
        errors.put("resourceName", ex.getResourceName());
        errors.put("fieldName", ex.getFieldName());
        errors.put("fieldValue", ex.getFieldValue());
        return buildErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, errors);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleDuplicateResource(
            DuplicateResourceException ex) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        Map<String, Object> errors = new HashMap<>();
        errors.put("resourceName", ex.getResourceName());
        errors.put("fieldName", ex.getFieldName());
        errors.put("fieldValue", ex.getFieldValue());
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT, errors);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.FORBIDDEN, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));
        log.warn("Validation error: {}", fieldErrors);
        return buildErrorResponse("Validation failed", HttpStatus.BAD_REQUEST, fieldErrors);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateKey(DuplicateKeyException ex) {
        log.warn("Duplicate key: {}", ex.getMessage());
        return buildErrorResponse("A resource with the given unique field already exists",
                HttpStatus.CONFLICT, null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildErrorResponse("An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    private <T> ResponseEntity<ApiResponse<T>> buildErrorResponse(String message, HttpStatus status, T errors) {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(errors)
                .statusCode(status.value())
                .timestamp(Instant.now())
                .build();
        return new ResponseEntity<>(response, status);
    }
}
