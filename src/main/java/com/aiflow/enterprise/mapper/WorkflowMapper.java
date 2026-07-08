package com.aiflow.enterprise.mapper;

import com.aiflow.enterprise.dto.request.WorkflowRequest;
import com.aiflow.enterprise.dto.response.WorkflowResponse;
import com.aiflow.enterprise.dto.response.step.WorkflowStepResponse;
import com.aiflow.enterprise.entity.Workflow;
import com.aiflow.enterprise.entity.embedded.WorkflowStep;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class})
public interface WorkflowMapper {

    @BeanMapping(builder = @org.mapstruct.Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "steps", expression = "java(mapStepRequests(request.getSteps()))")
    Workflow toEntity(WorkflowRequest request);

    WorkflowResponse toResponse(Workflow workflow);

    List<WorkflowResponse> toResponseList(List<Workflow> workflows);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            builder = @org.mapstruct.Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "steps", expression = "java(mapStepRequests(request.getSteps()))")
    void updateEntity(WorkflowRequest request, @MappingTarget Workflow workflow);

    default List<WorkflowStep> mapStepRequests(
            List<com.aiflow.enterprise.dto.request.step.WorkflowStepRequest> stepRequests) {
        if (stepRequests == null) return null;
        return stepRequests.stream()
                .map(s -> WorkflowStep.builder()
                        .stepId(UUID.randomUUID().toString())
                        .name(s.getName())
                        .description(s.getDescription())
                        .type(s.getType())
                        .order(s.getOrder())
                        .config(s.getConfig())
                        .dependsOn(s.getDependsOn())
                        .timeoutSeconds(s.getTimeoutSeconds())
                        .mandatory(s.getMandatory() != null ? s.getMandatory() : true)
                        .build())
                .toList();
    }

    WorkflowStepResponse toStepResponse(WorkflowStep step);

    List<WorkflowStepResponse> toStepResponseList(List<WorkflowStep> steps);
}
