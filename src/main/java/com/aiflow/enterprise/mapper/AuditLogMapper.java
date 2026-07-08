package com.aiflow.enterprise.mapper;

import com.aiflow.enterprise.dto.response.AuditLogResponse;
import com.aiflow.enterprise.entity.AuditLog;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog auditLog);

    List<AuditLogResponse> toResponseList(List<AuditLog> auditLogs);
}
