package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.request.RequestTypeRequest;
import com.aiflow.enterprise.dto.response.RequestTypeResponse;
import com.aiflow.enterprise.entity.RequestType;
import com.aiflow.enterprise.entity.Workflow;
import com.aiflow.enterprise.exception.DuplicateResourceException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.mapper.RequestTypeMapper;
import com.aiflow.enterprise.repository.RequestTypeRepository;
import com.aiflow.enterprise.repository.WorkflowRepository;
import com.aiflow.enterprise.service.RequestTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RequestTypeServiceImpl implements RequestTypeService {

    private static final Logger log = LoggerFactory.getLogger(RequestTypeServiceImpl.class);

    private final RequestTypeRepository requestTypeRepository;
    private final WorkflowRepository workflowRepository;
    private final RequestTypeMapper mapper;

    public RequestTypeServiceImpl(RequestTypeRepository requestTypeRepository,
                                  WorkflowRepository workflowRepository,
                                  RequestTypeMapper mapper) {
        this.requestTypeRepository = requestTypeRepository;
        this.workflowRepository = workflowRepository;
        this.mapper = mapper;
    }

    @Override
    public RequestTypeResponse createRequestType(RequestTypeRequest request, String createdBy) {
        if (requestTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("RequestType", "name", request.getName());
        }
        RequestType entity = mapper.toEntity(request);
        entity.setActive(true);
        entity.setCreatedBy(createdBy);

        if (request.getWorkflowId() != null) {
            workflowRepository.findById(request.getWorkflowId()).ifPresent(wf -> {
                entity.setWorkflowName(wf.getName());
            });
        }

        RequestType saved = requestTypeRepository.save(entity);
        log.info("Request type created: {} with id {}", saved.getName(), saved.getId());
        return mapper.toResponse(saved);
    }

    @Override
    public RequestTypeResponse updateRequestType(String id, RequestTypeRequest request) {
        RequestType existing = findOrThrow(id);
        if (!existing.getName().equals(request.getName())
                && requestTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("RequestType", "name", request.getName());
        }
        mapper.updateEntity(request, existing);

        if (request.getWorkflowId() != null) {
            workflowRepository.findById(request.getWorkflowId()).ifPresent(wf -> {
                existing.setWorkflowName(wf.getName());
            });
        }

        RequestType saved = requestTypeRepository.save(existing);
        log.info("Request type updated: {} with id {}", saved.getName(), saved.getId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RequestTypeResponse getRequestTypeById(String id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RequestTypeResponse> getAllRequestTypes(int page, int size, String category, Boolean active) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<RequestType> typePage;

        if (active != null) {
            typePage = requestTypeRepository.findByActive(active, pageable);
        } else if (category != null) {
            typePage = requestTypeRepository.findByCategory(category, pageable);
        } else {
            typePage = requestTypeRepository.findAll(pageable);
        }

        return typePage.map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RequestTypeResponse> getActiveRequestTypes() {
        return mapper.toResponseList(requestTypeRepository.findByActiveTrue());
    }

    @Override
    public void deleteRequestType(String id) {
        RequestType type = findOrThrow(id);
        requestTypeRepository.delete(type);
        log.info("Request type deleted: {} with id {}", type.getName(), id);
    }

    @Override
    public RequestTypeResponse toggleActive(String id) {
        RequestType type = findOrThrow(id);
        type.setActive(!type.isActive());
        RequestType saved = requestTypeRepository.save(type);
        log.info("Request type {} toggled active: {}", saved.getName(), saved.isActive());
        return mapper.toResponse(saved);
    }

    private RequestType findOrThrow(String id) {
        return requestTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RequestType", "id", id));
    }
}
