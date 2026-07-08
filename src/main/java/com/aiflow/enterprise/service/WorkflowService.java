package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.request.WorkflowRequest;
import com.aiflow.enterprise.dto.response.WorkflowResponse;
import com.aiflow.enterprise.enums.WorkflowStatus;
import org.springframework.data.domain.Page;

import java.util.List;

public interface WorkflowService {

    WorkflowResponse createWorkflow(WorkflowRequest request, String createdBy);

    WorkflowResponse updateWorkflow(String id, WorkflowRequest request);

    WorkflowResponse getWorkflowById(String id);

    Page<WorkflowResponse> getAllWorkflows(int page, int size, String status, String search, String tag, String category);

    void deleteWorkflow(String id);

    WorkflowResponse publishWorkflow(String id);

    WorkflowResponse archiveWorkflow(String id);

    String executeWorkflow(String id, String triggeredBy, java.util.Map<String, Object> inputParams);

    WorkflowResponse rollbackToVersion(String id, int version);

    List<Integer> getWorkflowVersions(String id);

    WorkflowResponse saveDraft(String id, WorkflowRequest request);
}
