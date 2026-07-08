package com.aiflow.enterprise.service;

import com.aiflow.enterprise.entity.Workflow;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import com.aiflow.enterprise.enums.WorkflowStatus;
import com.aiflow.enterprise.exception.BadRequestException;
import com.aiflow.enterprise.repository.WorkflowRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class WorkflowExportImportService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowExportImportService.class);

    private final WorkflowRepository workflowRepository;
    private final ObjectMapper objectMapper;

    public WorkflowExportImportService(WorkflowRepository workflowRepository,
                                       ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.objectMapper = objectMapper;
    }

    public String exportToJson(String workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new RuntimeException("Workflow not found: " + workflowId));
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                    Map.of("version", "1.0",
                            "workflow", Map.of(
                                    "name", workflow.getName(),
                                    "description", workflow.getDescription(),
                                    "category", workflow.getCategory(),
                                    "tags", workflow.getTags(),
                                    "steps", workflow.getSteps(),
                                    "metadata", workflow.getMetadata())));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to export workflow", e);
        }
    }

    public Workflow importFromJson(String json, String createdBy) {
        try {
            Map<String, Object> root = objectMapper.readValue(json, Map.class);
            String version = (String) root.getOrDefault("version", "1.0");
            Map<String, Object> workflowData = (Map<String, Object>) root.get("workflow");

            String name = (String) workflowData.get("name");
            if (workflowRepository.existsByName(name)) {
                throw new BadRequestException("Workflow with name '" + name + "' already exists");
            }

            List<WorkflowStep> steps = objectMapper.convertValue(
                    workflowData.get("steps"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, WorkflowStep.class));

            Workflow workflow = Workflow.builder()
                    .name(name)
                    .description((String) workflowData.get("description"))
                    .category((String) workflowData.get("category"))
                    .tags((List<String>) workflowData.get("tags"))
                    .steps(steps)
                    .metadata((Map<String, Object>) workflowData.get("metadata"))
                    .version(1)
                    .status(WorkflowStatus.DRAFT)
                    .createdBy(createdBy)
                    .build();

            Workflow saved = workflowRepository.save(workflow);
            log.info("Workflow imported: {} from JSON", saved.getName());
            return saved;

        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid workflow JSON format: " + e.getMessage());
        }
    }
}
