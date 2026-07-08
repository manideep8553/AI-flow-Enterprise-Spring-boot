package com.aiflow.enterprise.mapper;

import com.aiflow.enterprise.dto.response.ExecutionResponse;
import com.aiflow.enterprise.dto.response.log.ExecutionLogEntryResponse;
import com.aiflow.enterprise.entity.WorkflowExecution;
import com.aiflow.enterprise.entity.embedded.ExecutionLogEntry;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExecutionMapper {

    ExecutionResponse toResponse(WorkflowExecution execution);

    List<ExecutionResponse> toResponseList(List<WorkflowExecution> executions);

    ExecutionLogEntryResponse toLogResponse(ExecutionLogEntry entry);

    List<ExecutionLogEntryResponse> toLogResponseList(List<ExecutionLogEntry> entries);
}
