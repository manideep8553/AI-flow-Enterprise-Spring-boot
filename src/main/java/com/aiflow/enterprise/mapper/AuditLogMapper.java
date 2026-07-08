package com.aiflow.enterprise.mapper;

import com.aiflow.enterprise.dto.response.AuditLogResponse;
import com.aiflow.enterprise.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "createdAt", source = "createdAt")
    AuditLogResponse toResponse(AuditLog auditLog);

    List<AuditLogResponse> toResponseList(List<AuditLog> auditLogs);
}
