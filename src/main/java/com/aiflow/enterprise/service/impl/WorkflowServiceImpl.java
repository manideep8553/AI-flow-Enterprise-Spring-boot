package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.request.WorkflowRequest;
import com.aiflow.enterprise.dto.response.WorkflowResponse;
import com.aiflow.enterprise.entity.Workflow;
import com.aiflow.enterprise.enums.WorkflowStatus;
import com.aiflow.enterprise.exception.BadRequestException;
import com.aiflow.enterprise.exception.DuplicateResourceException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.mapper.WorkflowMapper;
import com.aiflow.enterprise.repository.WorkflowRepository;
import com.aiflow.enterprise.service.WorkflowExecutionService;
import com.aiflow.enterprise.service.WorkflowService;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkflowServiceImpl implements WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowServiceImpl.class);

    private final WorkflowRepository workflowRepository;
    private final WorkflowMapper workflowMapper;
    private final WorkflowExecutionService executionService;

    public WorkflowServiceImpl(WorkflowRepository workflowRepository,
                               WorkflowMapper workflowMapper,
                               WorkflowExecutionService executionService) {
        this.workflowRepository = workflowRepository;
        this.workflowMapper = workflowMapper;
        this.executionService = executionService;
    }

    @Override
    public WorkflowResponse createWorkflow(WorkflowRequest request, String createdBy) {
        if (workflowRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Workflow", "name", request.getName());
        }
        Workflow workflow = workflowMapper.toEntity(request);
        workflow.setCreatedBy(createdBy);
        workflow.setVersion(1);
        workflow.setStatus(WorkflowStatus.DRAFT);
        Workflow saved = workflowRepository.save(workflow);
        log.info("Workflow created: {} with id {}", saved.getName(), saved.getId());
        return workflowMapper.toResponse(saved);
    }

    @Override
    public WorkflowResponse updateWorkflow(String id, WorkflowRequest request) {
        Workflow existing = findWorkflowOrThrow(id);
        if (!existing.getName().equals(request.getName())
                && workflowRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Workflow", "name", request.getName());
        }
        workflowMapper.updateEntity(request, existing);
        existing.setVersion(existing.getVersion() + 1);
        Workflow saved = workflowRepository.save(existing);
        log.info("Workflow updated: {} with id {}", saved.getName(), saved.getId());
        return workflowMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowById(String id) {
        Workflow workflow = findWorkflowOrThrow(id);
        return workflowMapper.toResponse(workflow);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowResponse> getAllWorkflows(int page, int size, String status,
                                                   String search, String tag, String category) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Workflow> workflowPage;

        if (status != null) {
            WorkflowStatus workflowStatus = WorkflowStatus.valueOf(status.toUpperCase());
            workflowPage = workflowRepository.findByStatus(workflowStatus, pageable);
        } else if (tag != null) {
            workflowPage = workflowRepository.findByTagsContaining(tag, pageable);
        } else if (category != null) {
            workflowPage = workflowRepository.findByCategory(category, pageable);
        } else if (search != null) {
            workflowPage = workflowRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            workflowPage = workflowRepository.findAll(pageable);
        }

        return workflowPage.map(workflowMapper::toResponse);
    }

    @Override
    public void deleteWorkflow(String id) {
        Workflow workflow = findWorkflowOrThrow(id);
        workflowRepository.delete(workflow);
        log.info("Workflow deleted: {} with id {}", workflow.getName(), id);
    }

    @Override
    public WorkflowResponse archiveWorkflow(String id) {
        Workflow workflow = findWorkflowOrThrow(id);
        if (workflow.getStatus() == WorkflowStatus.ARCHIVED) {
            throw new BadRequestException("Workflow is already archived");
        }
        workflow.setStatus(WorkflowStatus.ARCHIVED);
        Workflow saved = workflowRepository.save(workflow);
        log.info("Workflow archived: {} with id {}", saved.getName(), id);
        return workflowMapper.toResponse(saved);
    }

    @Override
    public String executeWorkflow(String id, String triggeredBy, Map<String, Object> inputParams) {
        Workflow workflow = findWorkflowOrThrow(id);
        if (workflow.getStatus() != WorkflowStatus.PUBLISHED) {
            throw new BadRequestException("Only published workflows can be executed");
        }
        String executionId = executionService.createExecution(workflow, triggeredBy, inputParams);
        executionService.startExecution(executionId);
        log.info("Workflow execution started: workflowId={}, executionId={}", id, executionId);
        return executionId;
    }

    @Override
    public WorkflowResponse rollbackToVersion(String id, int version) {
        Workflow workflow = findWorkflowOrThrow(id);
        if (workflow.getVersionHistory() == null || workflow.getVersionHistory().isEmpty()) {
            throw new BadRequestException("No version history available for rollback");
        }
        Workflow.VersionSnapshot target = workflow.getVersionHistory().stream()
                .filter(v -> v.getVersion() == version)
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Version", "version", version));
        workflow.setSteps(new ArrayList<>(target.getSteps()));
        workflow.setMetadata(target.getMetadata() != null ? Map.copyOf(target.getMetadata()) : null);
        workflow.setVersion(version + 1);
        workflow.setStatus(WorkflowStatus.DRAFT);
        Workflow saved = workflowRepository.save(workflow);
        log.info("Workflow {} rolled back to version {}", id, version);
        return workflowMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Integer> getWorkflowVersions(String id) {
        Workflow workflow = findWorkflowOrThrow(id);
        List<Integer> versions = new ArrayList<>();
        versions.add(workflow.getVersion());
        if (workflow.getVersionHistory() != null) {
            versions.addAll(workflow.getVersionHistory().stream()
                    .map(Workflow.VersionSnapshot::getVersion)
                    .toList());
        }
        return versions.stream().sorted(java.util.Comparator.reverseOrder()).toList();
    }

    @Override
    public WorkflowResponse saveDraft(String id, WorkflowRequest request) {
        Workflow existing = findWorkflowOrThrow(id);
        workflowMapper.updateEntity(request, existing);
        existing.setStatus(WorkflowStatus.DRAFT);
        Workflow saved = workflowRepository.save(existing);
        log.info("Workflow draft saved: {}", id);
        return workflowMapper.toResponse(saved);
    }

    @Override
    public WorkflowResponse publishWorkflow(String id) {
        Workflow workflow = findWorkflowOrThrow(id);
        if (workflow.getStatus() == WorkflowStatus.PUBLISHED) {
            throw new BadRequestException("Workflow is already published");
        }
        Workflow.VersionSnapshot snapshot = Workflow.VersionSnapshot.builder()
                .version(workflow.getVersion())
                .steps(workflow.getSteps() != null ? List.copyOf(workflow.getSteps()) : null)
                .metadata(workflow.getMetadata() != null ? Map.copyOf(workflow.getMetadata()) : null)
                .archivedAt(Instant.now())
                .build();
        List<Workflow.VersionSnapshot> history = workflow.getVersionHistory();
        if (history == null) history = new ArrayList<>();
        history.add(snapshot);
        workflow.setVersionHistory(history);
        workflow.setVersion(workflow.getVersion() != null ? workflow.getVersion() + 1 : 2);
        workflow.setStatus(WorkflowStatus.PUBLISHED);
        Workflow saved = workflowRepository.save(workflow);
        log.info("Workflow published: {} with id {}, version {}", saved.getName(), id, saved.getVersion());
        return workflowMapper.toResponse(saved);
    }

    private Workflow findWorkflowOrThrow(String id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", id));
    }
}
