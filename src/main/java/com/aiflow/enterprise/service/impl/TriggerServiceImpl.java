package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.request.TriggerRequest;
import com.aiflow.enterprise.dto.response.TriggerResponse;
import com.aiflow.enterprise.entity.Trigger;
import com.aiflow.enterprise.entity.Workflow;
import com.aiflow.enterprise.enums.TriggerType;
import com.aiflow.enterprise.exception.BadRequestException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.mapper.TriggerMapper;
import com.aiflow.enterprise.repository.TriggerRepository;
import com.aiflow.enterprise.repository.WorkflowRepository;
import com.aiflow.enterprise.service.TriggerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TriggerServiceImpl implements TriggerService {

    private static final Logger log = LoggerFactory.getLogger(TriggerServiceImpl.class);

    private final TriggerRepository triggerRepository;
    private final WorkflowRepository workflowRepository;
    private final TriggerMapper triggerMapper;

    public TriggerServiceImpl(TriggerRepository triggerRepository,
                              WorkflowRepository workflowRepository,
                              TriggerMapper triggerMapper) {
        this.triggerRepository = triggerRepository;
        this.workflowRepository = workflowRepository;
        this.triggerMapper = triggerMapper;
    }

    @Override
    public TriggerResponse createTrigger(TriggerRequest request) {
        Workflow workflow = workflowRepository.findById(request.getWorkflowId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", request.getWorkflowId()));

        Trigger trigger = triggerMapper.toEntity(request);
        trigger.setWorkflowName(workflow.getName());
        if (trigger.getActive() == null) {
            trigger.setActive(true);
        }
        Trigger saved = triggerRepository.save(trigger);
        log.info("Trigger created: {} for workflow {}", saved.getName(), saved.getWorkflowId());
        return triggerMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TriggerResponse getTriggerById(String id) {
        Trigger trigger = findTriggerOrThrow(id);
        return triggerMapper.toResponse(trigger);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TriggerResponse> getAllTriggers(int page, int size, String workflowId,
                                                 String type, Boolean active) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Trigger> triggerPage;

        if (workflowId != null) {
            triggerPage = triggerRepository.findByWorkflowId(workflowId, pageable);
        } else if (type != null) {
            TriggerType triggerType = TriggerType.valueOf(type.toUpperCase());
            triggerPage = triggerRepository.findByType(triggerType, pageable);
        } else if (active != null) {
            triggerPage = triggerRepository.findByActive(active, pageable);
        } else {
            triggerPage = triggerRepository.findAll(pageable);
        }

        return triggerPage.map(triggerMapper::toResponse);
    }

    @Override
    public TriggerResponse updateTrigger(String id, TriggerRequest request) {
        Trigger existing = findTriggerOrThrow(id);
        triggerMapper.updateEntity(request, existing);

        if (request.getWorkflowId() != null
                && !request.getWorkflowId().equals(existing.getWorkflowId())) {
            Workflow workflow = workflowRepository.findById(request.getWorkflowId())
                    .orElseThrow(() -> new ResourceNotFoundException("Workflow", "id", request.getWorkflowId()));
            existing.setWorkflowName(workflow.getName());
        }

        Trigger saved = triggerRepository.save(existing);
        log.info("Trigger updated: {}", id);
        return triggerMapper.toResponse(saved);
    }

    @Override
    public void deleteTrigger(String id) {
        Trigger trigger = findTriggerOrThrow(id);
        triggerRepository.delete(trigger);
        log.info("Trigger deleted: {}", id);
    }

    @Override
    public TriggerResponse activateTrigger(String id) {
        Trigger trigger = findTriggerOrThrow(id);
        if (trigger.getActive()) {
            throw new BadRequestException("Trigger is already active");
        }
        trigger.setActive(true);
        Trigger saved = triggerRepository.save(trigger);
        log.info("Trigger activated: {}", id);
        return triggerMapper.toResponse(saved);
    }

    @Override
    public TriggerResponse deactivateTrigger(String id) {
        Trigger trigger = findTriggerOrThrow(id);
        if (!trigger.getActive()) {
            throw new BadRequestException("Trigger is already inactive");
        }
        trigger.setActive(false);
        Trigger saved = triggerRepository.save(trigger);
        log.info("Trigger deactivated: {}", id);
        return triggerMapper.toResponse(saved);
    }

    private Trigger findTriggerOrThrow(String id) {
        return triggerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trigger", "id", id));
    }
}
