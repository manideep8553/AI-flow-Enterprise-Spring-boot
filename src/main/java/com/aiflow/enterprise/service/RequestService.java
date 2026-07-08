package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.request.RequestActionRequest;
import com.aiflow.enterprise.dto.request.RequestCommentRequest;
import com.aiflow.enterprise.dto.request.RequestRequest;
import com.aiflow.enterprise.dto.response.RequestResponse;
import org.springframework.data.domain.Page;

import java.util.Map;

public interface RequestService {

    RequestResponse createRequest(RequestRequest request, String submittedBy);

    RequestResponse updateRequest(String id, RequestRequest request);

    RequestResponse getRequestById(String id);

    Page<RequestResponse> getMyRequests(String userId, int page, int size, String status, String requestTypeId);

    Page<RequestResponse> getRequestsForApproval(String approverId, int page, int size, String status);

    Page<RequestResponse> getAllRequests(int page, int size, String requestTypeId, String status,
                                          String submittedBy, String assignedTo, Boolean escalated);

    RequestResponse submitRequest(String id);

    RequestResponse approveRequest(String id, RequestActionRequest action, String approver);

    RequestResponse rejectRequest(String id, RequestActionRequest action, String approver);

    RequestResponse cancelRequest(String id, String userId);

    RequestResponse addComment(String id, RequestCommentRequest comment, String userId, String userName);

    RequestResponse updateFields(String id, Map<String, Object> fields);

    RequestResponse assignRequest(String id, String assignedTo);

    RequestResponse escalateRequest(String id, String reason);

    long getRequestCountByType(String requestTypeId);

    long getRequestCountByStatus(String status);

    void deleteRequest(String id);
}
