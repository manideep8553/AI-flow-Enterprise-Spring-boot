package com.aiflow.enterprise.service;

import com.aiflow.enterprise.dto.request.RequestTypeRequest;
import com.aiflow.enterprise.dto.response.RequestTypeResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface RequestTypeService {

    RequestTypeResponse createRequestType(RequestTypeRequest request, String createdBy);

    RequestTypeResponse updateRequestType(String id, RequestTypeRequest request);

    RequestTypeResponse getRequestTypeById(String id);

    Page<RequestTypeResponse> getAllRequestTypes(int page, int size, String category, Boolean active);

    List<RequestTypeResponse> getActiveRequestTypes();

    void deleteRequestType(String id);

    RequestTypeResponse toggleActive(String id);
}
