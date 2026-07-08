package com.aiflow.enterprise.org.service;

import com.aiflow.enterprise.entity.Workflow;
import com.aiflow.enterprise.entity.WorkflowTemplate;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.enums.WorkflowStatus;
import com.aiflow.enterprise.exception.DuplicateResourceException;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.org.repository.WorkflowTemplateRepository;
import com.aiflow.enterprise.repository.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class WorkflowTemplateService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTemplateService.class);

    private final WorkflowTemplateRepository templateRepository;
    private final WorkflowRepository workflowRepository;

    public WorkflowTemplateService(WorkflowTemplateRepository templateRepository,
                                   WorkflowRepository workflowRepository) {
        this.templateRepository = templateRepository;
        this.workflowRepository = workflowRepository;
    }

    public WorkflowTemplate create(String name, String description, String category,
                                    List<String> tags, List<WorkflowStep> steps,
                                    Map<String, Object> metadata, String createdBy) {
        if (templateRepository.findByName(name).isPresent()) {
            throw new DuplicateResourceException("WorkflowTemplate", "name", name);
        }
        WorkflowTemplate template = WorkflowTemplate.builder().name(name)
                .description(description).category(category).tags(tags).steps(steps)
                .metadata(metadata).published(false).usageCount(0L).createdBy(createdBy).build();
        return templateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public WorkflowTemplate getById(String id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkflowTemplate", "id", id));
    }

    @Transactional(readOnly = true)
    public Page<WorkflowTemplate> getAll(int page, int size, String category, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "usageCount"));
        if (search != null) return templateRepository.findByNameContainingIgnoreCase(search, pageable);
        if (category != null) return templateRepository.findByCategory(category, pageable);
        return templateRepository.findAll(pageable);
    }

    public Workflow createWorkflowFromTemplate(String templateId, String createdBy) {
        WorkflowTemplate template = getById(templateId);
        String newName = template.getName() + " (from template)";
        int v = 1;
        while (workflowRepository.existsByName(newName)) {
            newName = template.getName() + " (from template " + (++v) + ")";
        }
        Workflow workflow = Workflow.builder().name(newName)
                .description(template.getDescription()).category(template.getCategory())
                .tags(template.getTags()).steps(cloneSteps(template.getSteps()))
                .metadata(template.getMetadata()).version(1)
                .status(WorkflowStatus.DRAFT).createdBy(createdBy).build();
        Workflow saved = workflowRepository.save(workflow);

        template.setUsageCount(template.getUsageCount() != null
                ? template.getUsageCount() + 1 : 1);
        templateRepository.save(template);

        log.info("Workflow '{}' created from template '{}'", saved.getName(), template.getName());
        return saved;
    }

    public void publishTemplate(String id) {
        WorkflowTemplate t = getById(id);
        t.setPublished(true);
        templateRepository.save(t);
    }

    public void delete(String id) { templateRepository.delete(getById(id)); }

    private List<WorkflowStep> cloneSteps(List<WorkflowStep> steps) {
        if (steps == null) return null;
        return steps.stream().map(s -> WorkflowStep.builder()
                .stepId(java.util.UUID.randomUUID().toString()).name(s.getName())
                .description(s.getDescription()).type(s.getType()).order(s.getOrder())
                .config(s.getConfig()).dependsOn(s.getDependsOn())
                .timeoutSeconds(s.getTimeoutSeconds()).mandatory(s.getMandatory()).build())
                .toList();
    }
}
