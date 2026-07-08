package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.request.RequestActionRequest;
import com.aiflow.enterprise.dto.request.RequestCommentRequest;
import com.aiflow.enterprise.dto.request.RequestRequest;
import com.aiflow.enterprise.dto.response.RequestResponse;
import com.aiflow.enterprise.entity.Request;
import com.aiflow.enterprise.entity.RequestType;
import com.aiflow.enterprise.entity.Workflow;
import com.aiflow.enterprise.entity.embedded.ApprovalEntry;
import com.aiflow.enterprise.entity.embedded.CommentEntry;
import com.aiflow.enterprise.entity.embedded.FileAttachment;
import com.aiflow.enterprise.entity.embedded.StatusChangeEntry;
import com.aiflow.enterprise.enums.ExecutionStatus;
import com.aiflow.enterprise.enums.RequestStatus;
import com.aiflow.enterprise.exception.BadRequestException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.mapper.RequestMapper;
import com.aiflow.enterprise.repository.RequestRepository;
import com.aiflow.enterprise.repository.RequestTypeRepository;
import com.aiflow.enterprise.repository.WorkflowRepository;
import com.aiflow.enterprise.service.RequestService;
import com.aiflow.enterprise.service.WorkflowExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class RequestServiceImpl implements RequestService {

    private static final Logger log = LoggerFactory.getLogger(RequestServiceImpl.class);

    private final RequestRepository requestRepository;
    private final RequestTypeRepository requestTypeRepository;
    private final WorkflowRepository workflowRepository;
    private final RequestMapper mapper;
    private final WorkflowExecutionService executionService;

    public RequestServiceImpl(RequestRepository requestRepository,
                              RequestTypeRepository requestTypeRepository,
                              WorkflowRepository workflowRepository,
                              RequestMapper mapper,
                              WorkflowExecutionService executionService) {
        this.requestRepository = requestRepository;
        this.requestTypeRepository = requestTypeRepository;
        this.workflowRepository = workflowRepository;
        this.mapper = mapper;
        this.executionService = executionService;
    }

    @Override
    public RequestResponse createRequest(RequestRequest request, String submittedBy) {
        RequestType requestType = requestTypeRepository.findById(request.getRequestTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("RequestType", "id", request.getRequestTypeId()));

        if (!requestType.isActive()) {
            throw new BadRequestException("Request type is not active: " + requestType.getName());
        }

        Request entity = mapper.toEntity(request);
        entity.setStatus(RequestStatus.DRAFT);
        entity.setSubmittedBy(submittedBy);
        entity.setSubmittedAt(Instant.now());
        entity.setRequestTypeName(requestType.getName());
        entity.setWorkflowId(requestType.getWorkflowId());
        entity.setWorkflowName(requestType.getWorkflowName());
        entity.setPriority(request.getPriority() != null ? request.getPriority() : requestType.getDefaultPriority());
        entity.setAttachments(new ArrayList<>());
        entity.setComments(new ArrayList<>());
        entity.setApprovalHistory(new ArrayList<>());
        entity.setStatusHistory(new ArrayList<>());

        addStatusChange(entity, null, RequestStatus.DRAFT, submittedBy, "Request created");

        Request saved = requestRepository.save(entity);
        log.info("Request created: {} type={} by {}", saved.getTitle(), requestType.getName(), submittedBy);
        return mapper.toResponse(saved);
    }

    @Override
    public RequestResponse updateRequest(String id, RequestRequest request) {
        Request existing = findOrThrow(id);
        if (existing.getStatus() != RequestStatus.DRAFT) {
            throw new BadRequestException("Only draft requests can be edited");
        }
        mapper.updateEntity(request, existing);
        existing.setVersion(existing.getVersion() + 1);

        Request saved = requestRepository.save(existing);
        log.info("Request updated: {}", id);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RequestResponse getRequestById(String id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequestResponse> getMyRequests(String userId, int page, int size,
                                                String status, String requestTypeId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Request> requestPage;

        if (status != null && requestTypeId != null) {
            RequestStatus rs = RequestStatus.valueOf(status.toUpperCase());
            requestPage = requestRepository.findBySubmittedByAndRequestTypeId(userId, requestTypeId, pageable);
        } else if (status != null) {
            RequestStatus rs = RequestStatus.valueOf(status.toUpperCase());
            requestPage = requestRepository.findBySubmittedByAndStatus(userId, rs, pageable);
        } else if (requestTypeId != null) {
            requestPage = requestRepository.findByRequestTypeId(requestTypeId, pageable);
        } else {
            requestPage = requestRepository.findBySubmittedBy(userId, pageable);
        }

        return requestPage.map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequestResponse> getRequestsForApproval(String approverId, int page, int size, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Request> requestPage;

        if (status != null) {
            RequestStatus rs = RequestStatus.valueOf(status.toUpperCase());
            requestPage = requestRepository.findByCurrentApproverAndStatus(approverId, rs, pageable);
        } else {
            requestPage = requestRepository.findByCurrentApprover(approverId, pageable);
        }

        return requestPage.map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequestResponse> getAllRequests(int page, int size, String requestTypeId,
                                                 String status, String submittedBy,
                                                 String assignedTo, Boolean escalated) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Request> requestPage;

        if (requestTypeId != null && status != null) {
            RequestStatus rs = RequestStatus.valueOf(status.toUpperCase());
            requestPage = requestRepository.findByRequestTypeIdAndStatus(requestTypeId, rs, pageable);
        } else if (requestTypeId != null) {
            requestPage = requestRepository.findByRequestTypeId(requestTypeId, pageable);
        } else if (status != null) {
            RequestStatus rs = RequestStatus.valueOf(status.toUpperCase());
            requestPage = requestRepository.findByStatus(rs, pageable);
        } else if (submittedBy != null) {
            requestPage = requestRepository.findBySubmittedBy(submittedBy, pageable);
        } else if (assignedTo != null) {
            requestPage = requestRepository.findByAssignedTo(assignedTo, pageable);
        } else if (Boolean.TRUE.equals(escalated)) {
            requestPage = requestRepository.findByEscalatedTrue(pageable);
        } else {
            requestPage = requestRepository.findAll(pageable);
        }

        return requestPage.map(mapper::toResponse);
    }

    @Override
    public RequestResponse submitRequest(String id) {
        Request request = findOrThrow(id);
        if (request.getStatus() != RequestStatus.DRAFT) {
            throw new BadRequestException("Only draft requests can be submitted");
        }

        request.setStatus(RequestStatus.SUBMITTED);
        addStatusChange(request, RequestStatus.DRAFT, RequestStatus.SUBMITTED,
                request.getSubmittedBy(), "Request submitted");

        if (request.getWorkflowId() != null) {
            Map<String, Object> inputParams = new HashMap<>();
            inputParams.put("requestId", request.getId());
            inputParams.put("requestTitle", request.getTitle());
            inputParams.put("requestType", request.getRequestTypeName());
            inputParams.put("submittedBy", request.getSubmittedBy());
            if (request.getFields() != null) inputParams.putAll(request.getFields());

            Workflow wf = workflowRepository.findById(request.getWorkflowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", request.getWorkflowId()));
            String executionId = executionService.createExecution(wf, request.getSubmittedBy(), inputParams);
            request.setWorkflowExecutionId(executionId);
            request.setStatus(RequestStatus.PENDING_APPROVAL);
            addStatusChange(request, RequestStatus.SUBMITTED, RequestStatus.PENDING_APPROVAL,
                    "system", "Workflow execution started");
        }

        Request saved = requestRepository.save(request);
        log.info("Request submitted: {} status={}", id, saved.getStatus());
        return mapper.toResponse(saved);
    }

    @Override
    public RequestResponse approveRequest(String id, RequestActionRequest action, String approver) {
        Request request = findOrThrow(id);
        validateAction(request, RequestStatus.APPROVED, approver);

        request.setStatus(RequestStatus.APPROVED);
        request.setCompletedAt(Instant.now());

        addApprovalEntry(request, action, "APPROVED", approver);
        addStatusChange(request, request.getStatus(), RequestStatus.APPROVED, approver, action.getComment());

        Request saved = requestRepository.save(request);
        log.info("Request approved: {} by {}", id, approver);
        return mapper.toResponse(saved);
    }

    @Override
    public RequestResponse rejectRequest(String id, RequestActionRequest action, String approver) {
        Request request = findOrThrow(id);
        validateAction(request, RequestStatus.REJECTED, approver);

        request.setStatus(RequestStatus.REJECTED);
        request.setCompletedAt(Instant.now());

        addApprovalEntry(request, action, "REJECTED", approver);
        addStatusChange(request, request.getStatus(), RequestStatus.REJECTED, approver, action.getComment());

        Request saved = requestRepository.save(request);
        log.info("Request rejected: {} by {}", id, approver);
        return mapper.toResponse(saved);
    }

    @Override
    public RequestResponse cancelRequest(String id, String userId) {
        Request request = findOrThrow(id);
        if (request.getStatus() == RequestStatus.COMPLETED
                || request.getStatus() == RequestStatus.CANCELLED
                || request.getStatus() == RequestStatus.REJECTED) {
            throw new BadRequestException("Cannot cancel request in status: " + request.getStatus());
        }

        if (!request.getSubmittedBy().equals(userId)) {
            RequestType requestType = requestTypeRepository.findById(request.getRequestTypeId()).orElse(null);
            if (requestType != null && !requestType.isAllowRequesterCancellation()) {
                throw new BadRequestException("Requester cancellation is not allowed for this request type");
            }
        }

        RequestStatus previousStatus = request.getStatus();
        request.setStatus(RequestStatus.CANCELLED);
        request.setCompletedAt(Instant.now());
        addStatusChange(request, previousStatus, RequestStatus.CANCELLED, userId, "Request cancelled");

        Request saved = requestRepository.save(request);
        log.info("Request cancelled: {} by {}", id, userId);
        return mapper.toResponse(saved);
    }

    @Override
    public RequestResponse addComment(String id, RequestCommentRequest commentReq,
                                       String userId, String userName) {
        Request request = findOrThrow(id);

        CommentEntry comment = CommentEntry.builder()
                .id(UUID.randomUUID().toString())
                .author(userId)
                .authorName(userName)
                .text(commentReq.getText())
                .internal(commentReq.isInternal())
                .createdAt(Instant.now())
                .build();

        if (request.getComments() == null) request.setComments(new ArrayList<>());
        request.getComments().add(comment);
        request.setVersion(request.getVersion() + 1);

        Request saved = requestRepository.save(request);
        log.info("Comment added to request: {} by {}", id, userId);
        return mapper.toResponse(saved);
    }

    @Override
    public RequestResponse updateFields(String id, Map<String, Object> fields) {
        Request request = findOrThrow(id);
        if (request.getFields() == null) request.setFields(new HashMap<>());
        request.getFields().putAll(fields);
        request.setVersion(request.getVersion() + 1);

        Request saved = requestRepository.save(request);
        log.info("Fields updated for request: {}", id);
        return mapper.toResponse(saved);
    }

    @Override
    public RequestResponse assignRequest(String id, String assignedTo) {
        Request request = findOrThrow(id);
        request.setAssignedTo(assignedTo);
        request.setCurrentApprover(assignedTo);

        Request saved = requestRepository.save(request);
        log.info("Request assigned: {} to {}", id, assignedTo);
        return mapper.toResponse(saved);
    }

    @Override
    public RequestResponse escalateRequest(String id, String reason) {
        Request request = findOrThrow(id);
        request.setEscalated(true);
        request.setEscalatedAt(Instant.now());
        request.setEscalationReason(reason);
        request.setStatus(RequestStatus.ESCALATED);

        addStatusChange(request, request.getStatus(), RequestStatus.ESCALATED, "system", reason);

        Request saved = requestRepository.save(request);
        log.info("Request escalated: {} reason={}", id, reason);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public long getRequestCountByType(String requestTypeId) {
        return requestRepository.countByRequestTypeId(requestTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getRequestCountByStatus(String status) {
        return requestRepository.countByStatus(RequestStatus.valueOf(status.toUpperCase()));
    }

    @Override
    public void deleteRequest(String id) {
        Request request = findOrThrow(id);
        requestRepository.delete(request);
        log.info("Request deleted: {}", id);
    }

    private void validateAction(Request request, RequestStatus targetStatus, String approver) {
        if (request.getStatus() != RequestStatus.PENDING_APPROVAL
                && request.getStatus() != RequestStatus.SUBMITTED) {
            throw new BadRequestException(
                    "Request must be in PENDING_APPROVAL or SUBMITTED status to take action");
        }

        if (request.getCurrentApprover() != null
                && !request.getCurrentApprover().equals(approver)) {
            throw new BadRequestException("You are not the current approver for this request");
        }
    }

    private void addApprovalEntry(Request request, RequestActionRequest action,
                                   String result, String approver) {
        ApprovalEntry entry = ApprovalEntry.builder()
                .stepId("approval")
                .stepName("Approval")
                .approver(approver)
                .action(result)
                .comment(action != null ? action.getComment() : null)
                .timestamp(Instant.now())
                .build();
        if (request.getApprovalHistory() == null) request.setApprovalHistory(new ArrayList<>());
        request.getApprovalHistory().add(entry);
    }

    private void addStatusChange(Request request, RequestStatus from, RequestStatus to,
                                  String changedBy, String reason) {
        StatusChangeEntry entry = StatusChangeEntry.builder()
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedBy)
                .reason(reason)
                .timestamp(Instant.now())
                .build();
        if (request.getStatusHistory() == null) request.setStatusHistory(new ArrayList<>());
        request.getStatusHistory().add(entry);
    }

    private Request findOrThrow(String id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request", "id", id));
    }
}
