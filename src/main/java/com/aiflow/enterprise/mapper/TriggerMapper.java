package com.aiflow.enterprise.mapper;

import com.aiflow.enterprise.dto.request.TriggerRequest;
import com.aiflow.enterprise.dto.response.TriggerResponse;
import com.aiflow.enterprise.entity.Trigger;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TriggerMapper {

    @BeanMapping(builder = @org.mapstruct.Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lastTriggeredAt", ignore = true)
    @Mapping(target = "triggerCount", constant = "0L")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "workflowName", ignore = true)
    Trigger toEntity(TriggerRequest request);

    TriggerResponse toResponse(Trigger trigger);

    List<TriggerResponse> toResponseList(List<Trigger> triggers);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            builder = @org.mapstruct.Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workflowId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(TriggerRequest request, @MappingTarget Trigger trigger);
}
