package com.aiflow.enterprise.service.impl;

import com.aiflow.enterprise.dto.request.AuditExportRequest;
import com.aiflow.enterprise.dto.request.AuditLogFilterRequest;
import com.aiflow.enterprise.dto.response.AuditLogResponse;
import com.aiflow.enterprise.dto.response.AuditSummaryResponse;
import com.aiflow.enterprise.dto.response.ComplianceReportResponse;
import com.aiflow.enterprise.entity.AuditLog;
import com.aiflow.enterprise.entity.ComplianceReport;
import com.aiflow.enterprise.exception.ResourceNotFoundException;
import com.aiflow.enterprise.mapper.AuditLogMapper;
import com.aiflow.enterprise.repository.AuditLogRepository;
import com.aiflow.enterprise.repository.ComplianceReportRepository;
import com.aiflow.enterprise.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AuditLogServiceImpl implements AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogServiceImpl.class);

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("UTC"));

    private final AuditLogRepository auditLogRepository;
    private final ComplianceReportRepository complianceReportRepository;
    private final AuditLogMapper auditLogMapper;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository,
                                ComplianceReportRepository complianceReportRepository,
                                AuditLogMapper auditLogMapper,
                                MongoTemplate mongoTemplate) {
        this.auditLogRepository = auditLogRepository;
        this.complianceReportRepository = complianceReportRepository;
        this.auditLogMapper = auditLogMapper;
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public Page<AuditLogResponse> getAllAuditLogs(int page, int size, String entityType,
                                                   String entityId, String performedBy,
                                                   String action, Instant from, Instant to) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditLogPage;

        if (entityType != null) {
            auditLogPage = auditLogRepository.findByEntityType(entityType, pageable);
        } else if (entityId != null) {
            auditLogPage = auditLogRepository.findByEntityId(entityId, pageable);
        } else if (performedBy != null) {
            auditLogPage = auditLogRepository.findByPerformedBy(performedBy, pageable);
        } else if (action != null) {
            auditLogPage = auditLogRepository.findByAction(action, pageable);
        } else if (from != null && to != null) {
            auditLogPage = auditLogRepository.findByTimestampBetween(from, to, pageable);
        } else {
            auditLogPage = auditLogRepository.findAll(pageable);
        }

        return auditLogPage.map(auditLogMapper::toResponse);
    }

    @Override
    public Page<AuditLogResponse> getAuditLogs(AuditLogFilterRequest filter) {
        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(getSortDirection(filter.getSortDirection()), filter.getSortBy() != null ? filter.getSortBy() : "timestamp")
        );

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (filter.getAction() != null) {
            criteriaList.add(Criteria.where("action").is(filter.getAction()));
        }
        if (filter.getActions() != null && !filter.getActions().isEmpty()) {
            criteriaList.add(Criteria.where("action").in(filter.getActions()));
        }
        if (filter.getEntityType() != null) {
            criteriaList.add(Criteria.where("entityType").is(filter.getEntityType()));
        }
        if (filter.getEntityId() != null) {
            criteriaList.add(Criteria.where("entityId").is(filter.getEntityId()));
        }
        if (filter.getPerformedBy() != null) {
            criteriaList.add(Criteria.where("performedBy").is(filter.getPerformedBy()));
        }
        if (filter.getCorrelationId() != null) {
            criteriaList.add(Criteria.where("correlationId").is(filter.getCorrelationId()));
        }
        if (filter.getSessionId() != null) {
            criteriaList.add(Criteria.where("sessionId").is(filter.getSessionId()));
        }
        if (filter.getRequestId() != null) {
            criteriaList.add(Criteria.where("requestId").is(filter.getRequestId()));
        }
        if (filter.getWorkflowId() != null) {
            criteriaList.add(Criteria.where("workflowId").is(filter.getWorkflowId()));
        }
        if (filter.getExecutionId() != null) {
            criteriaList.add(Criteria.where("executionId").is(filter.getExecutionId()));
        }
        if (filter.getIpAddress() != null) {
            criteriaList.add(Criteria.where("ipAddress").is(filter.getIpAddress()));
        }
        if (filter.getUserAgent() != null) {
            criteriaList.add(Criteria.where("userAgent").regex(filter.getUserAgent(), "i"));
        }
        if (filter.getSuccess() != null) {
            criteriaList.add(Criteria.where("success").is(filter.getSuccess()));
        }
        if (filter.getImmutable() != null) {
            criteriaList.add(Criteria.where("immutable").is(filter.getImmutable()));
        }
        if (filter.getOrganizationId() != null) {
            criteriaList.add(Criteria.where("organizationId").is(filter.getOrganizationId()));
        }
        if (filter.getEndpoint() != null) {
            criteriaList.add(Criteria.where("endpoint").regex(filter.getEndpoint(), "i"));
        }
        if (filter.getHttpMethod() != null) {
            criteriaList.add(Criteria.where("httpMethod").is(filter.getHttpMethod().toUpperCase()));
        }
        if (filter.getFrom() != null && filter.getTo() != null) {
            criteriaList.add(Criteria.where("timestamp").gte(filter.getFrom()).lte(filter.getTo()));
        } else if (filter.getFrom() != null) {
            criteriaList.add(Criteria.where("timestamp").gte(filter.getFrom()));
        } else if (filter.getTo() != null) {
            criteriaList.add(Criteria.where("timestamp").lte(filter.getTo()));
        }
        if (filter.getSearchTerm() != null && !filter.getSearchTerm().isEmpty()) {
            String searchTerm = filter.getSearchTerm();
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("action").regex(searchTerm, "i"),
                    Criteria.where("entityType").regex(searchTerm, "i"),
                    Criteria.where("entityId").regex(searchTerm, "i"),
                    Criteria.where("performedBy").regex(searchTerm, "i"),
                    Criteria.where("correlationId").regex(searchTerm, "i"),
                    Criteria.where("ipAddress").regex(searchTerm, "i"),
                    Criteria.where("failureReason").regex(searchTerm, "i")
            ));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, AuditLog.class);
        query.with(pageable);

        List<AuditLog> auditLogs = mongoTemplate.find(query, AuditLog.class);

        List<AuditLogResponse> responses = auditLogMapper.toResponseList(auditLogs);

        return new PageImpl<>(responses, pageable, total);
    }

    @Override
    public AuditLogResponse getAuditLogById(String id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", "id", id));
        return auditLogMapper.toResponse(auditLog);
    }

    @Override
    public List<AuditLogResponse> getAuditLogsByCorrelationId(String correlationId) {
        return auditLogMapper.toResponseList(
                auditLogRepository.findByCorrelationIdOrderByTimestampAsc(correlationId));
    }

    @Override
    public List<AuditLogResponse> getAuditLogsBySessionId(String sessionId) {
        return auditLogMapper.toResponseList(
                auditLogRepository.findBySessionIdOrderByTimestampAsc(sessionId));
    }

    @Override
    public List<AuditLogResponse> getAuditLogsByEntity(String entityType, String entityId) {
        return auditLogMapper.toResponseList(
                auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId));
    }

    @Override
    public List<AuditLogResponse> getAuditLogsByWorkflowId(String workflowId) {
        Page<AuditLog> page = auditLogRepository.findByWorkflowId(workflowId, PageRequest.of(0, 1000, Sort.by(Sort.Direction.ASC, "timestamp")));
        return auditLogMapper.toResponseList(page.getContent());
    }

    @Override
    public List<AuditLogResponse> getAuditLogsByExecutionId(String executionId) {
        Page<AuditLog> page = auditLogRepository.findByExecutionId(executionId, PageRequest.of(0, 1000, Sort.by(Sort.Direction.ASC, "timestamp")));
        return auditLogMapper.toResponseList(page.getContent());
    }

    @Override
    public AuditSummaryResponse getAuditSummary(Instant from, Instant to) {
        long totalLogs;
        long failures;
        long successes;
        long uniqueUsers = 0;
        long uniqueActions = 0;
        Map<String, Long> actionBreakdown = new LinkedHashMap<>();
        Map<String, Long> entityTypeBreakdown = new LinkedHashMap<>();
        Map<String, Long> userActivityTop = new LinkedHashMap<>();
        List<AuditSummaryResponse.TimeSeriesPoint> timeSeries = new ArrayList<>();

        if (from != null && to != null) {
            totalLogs = auditLogRepository.countByTimestampBetween(from, to);
            successes = totalLogs > 0 ? auditLogRepository.countBySuccess(true) : 0;
            failures = totalLogs - successes;
        } else {
            totalLogs = auditLogRepository.count();
            successes = totalLogs > 0 ? auditLogRepository.countBySuccess(true) : 0;
            failures = totalLogs - successes;
        }

        try {
            List<AuditLogRepository.ActionCount> actionCounts = auditLogRepository.countByActionGrouped();
            for (AuditLogRepository.ActionCount ac : actionCounts) {
                if (ac.get_Id() != null) {
                    actionBreakdown.put(ac.get_Id(), ac.getCount());
                }
            }
            uniqueActions = actionCounts.size();

            List<AuditLogRepository.EntityTypeCount> entityCounts = auditLogRepository.countByEntityTypeGrouped();
            for (AuditLogRepository.EntityTypeCount ec : entityCounts) {
                if (ec.get_Id() != null) {
                    entityTypeBreakdown.put(ec.get_Id(), ec.getCount());
                }
            }

            List<AuditLogRepository.UserActivityCount> topUsers = auditLogRepository.findTopActiveUsers();
            for (AuditLogRepository.UserActivityCount uc : topUsers) {
                if (uc.get_Id() != null) {
                    userActivityTop.put(uc.get_Id(), uc.getCount());
                }
            }
            uniqueUsers = auditLogRepository.findTopActiveUsers().size();

            List<AuditLogRepository.DailyAuditStats> stats;
            if (from != null && to != null) {
                stats = auditLogRepository.getDailyAuditStatsBetween(from, to);
            } else {
                stats = auditLogRepository.getDailyAuditStats();
            }
            for (AuditLogRepository.DailyAuditStats s : stats) {
                timeSeries.add(AuditSummaryResponse.TimeSeriesPoint.builder()
                        .period(s.get_Id())
                        .count(s.getCount())
                        .successCount(s.getSuccessCount())
                        .failureCount(s.getFailureCount())
                        .build());
            }
        } catch (Exception e) {
            log.warn("Could not compute full audit summary, returning partial: {}", e.getMessage());
        }

        return AuditSummaryResponse.builder()
                .totalLogs(totalLogs)
                .successCount(successes)
                .failureCount(failures)
                .uniqueUsers(uniqueUsers)
                .uniqueActions(uniqueActions)
                .actionBreakdown(actionBreakdown)
                .entityTypeBreakdown(entityTypeBreakdown)
                .userActivityTop(userActivityTop)
                .timeSeries(timeSeries)
                .from(from)
                .to(to)
                .build();
    }

    @Override
    public void exportAuditLogs(AuditExportRequest request, OutputStream outputStream) {
        try {
            Query query = buildExportQuery(request);
            query.with(Sort.by(Sort.Direction.ASC, "timestamp"));

            List<AuditLog> auditLogs = mongoTemplate.find(query, AuditLog.class);

            String format = request.getFormat() != null ? request.getFormat().toLowerCase() : "csv";

            switch (format) {
                case "json" -> exportAsJson(auditLogs, outputStream);
                case "csv" -> exportAsCsv(auditLogs, outputStream);
                default -> exportAsCsv(auditLogs, outputStream);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to export audit logs: " + e.getMessage(), e);
        }
    }

    @Override
    public ComplianceReportResponse generateComplianceReport(String reportType, Instant from, Instant to, String generatedBy) {
        AuditSummaryResponse summary = getAuditSummary(from, to);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("reportType", reportType);
        parameters.put("from", from != null ? from.toString() : null);
        parameters.put("to", to != null ? to.toString() : null);

        Map<String, Object> reportSummary = new HashMap<>();
        reportSummary.put("totalLogs", summary.getTotalLogs());
        reportSummary.put("successCount", summary.getSuccessCount());
        reportSummary.put("failureCount", summary.getFailureCount());
        reportSummary.put("uniqueUsers", summary.getUniqueUsers());
        reportSummary.put("uniqueActions", summary.getUniqueActions());
        reportSummary.put("actionBreakdown", summary.getActionBreakdown());
        reportSummary.put("entityTypeBreakdown", summary.getEntityTypeBreakdown());
        reportSummary.put("timeSeries", summary.getTimeSeries());

        ComplianceReport report = ComplianceReport.builder()
                .reportType(reportType)
                .title("Compliance Report: " + reportType)
                .description("Generated compliance report for " + reportType)
                .parameters(parameters)
                .summary(reportSummary)
                .status("COMPLETED")
                .format("JSON")
                .recordCount(summary.getTotalLogs())
                .generatedBy(generatedBy)
                .from(from)
                .to(to)
                .generatedAt(Instant.now())
                .build();

        report = complianceReportRepository.save(report);

        return mapToComplianceReportResponse(report);
    }

    @Override
    public long getTotalAuditLogCount() {
        return auditLogRepository.count();
    }

    @Override
    public long getFailureCount() {
        return auditLogRepository.countBySuccess(false);
    }

    private Query buildExportQuery(AuditExportRequest request) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (request.getActions() != null && !request.getActions().isEmpty()) {
            criteriaList.add(Criteria.where("action").in(request.getActions()));
        }
        if (request.getEntityType() != null) {
            criteriaList.add(Criteria.where("entityType").is(request.getEntityType()));
        }
        if (request.getEntityId() != null) {
            criteriaList.add(Criteria.where("entityId").is(request.getEntityId()));
        }
        if (request.getPerformedBy() != null) {
            criteriaList.add(Criteria.where("performedBy").is(request.getPerformedBy()));
        }
        if (request.getCorrelationId() != null) {
            criteriaList.add(Criteria.where("correlationId").is(request.getCorrelationId()));
        }
        if (request.getWorkflowId() != null) {
            criteriaList.add(Criteria.where("workflowId").is(request.getWorkflowId()));
        }
        if (request.getExecutionId() != null) {
            criteriaList.add(Criteria.where("executionId").is(request.getExecutionId()));
        }
        if (request.getFrom() != null && request.getTo() != null) {
            criteriaList.add(Criteria.where("timestamp").gte(request.getFrom()).lte(request.getTo()));
        } else if (request.getFrom() != null) {
            criteriaList.add(Criteria.where("timestamp").gte(request.getFrom()));
        } else if (request.getTo() != null) {
            criteriaList.add(Criteria.where("timestamp").lte(request.getTo()));
        }
        if (request.getSuccess() != null) {
            criteriaList.add(Criteria.where("success").is(request.getSuccess()));
        }
        if (request.getOrganizationId() != null) {
            criteriaList.add(Criteria.where("organizationId").is(request.getOrganizationId()));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        return query;
    }

    private void exportAsJson(List<AuditLog> auditLogs, OutputStream outputStream) throws Exception {
        List<AuditLogResponse> responses = auditLogMapper.toResponseList(auditLogs);
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responses);
        outputStream.write(json.getBytes(StandardCharsets.UTF_8));
    }

    private void exportAsCsv(List<AuditLog> auditLogs, OutputStream outputStream) throws Exception {
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            writer.write("ID,Action,EntityType,EntityId,PerformedBy,Timestamp,IPAddress,UserAgent,CorrelationId,SessionId,WorkflowId,ExecutionId,Endpoint,HTTPMethod,Success,FailureReason,DurationMs\n");
            for (AuditLog log : auditLogs) {
                writer.write(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%b,\"%s\",%d\n",
                        escapeCsv(log.getId()),
                        escapeCsv(log.getAction()),
                        escapeCsv(log.getEntityType()),
                        escapeCsv(log.getEntityId()),
                        escapeCsv(log.getPerformedBy()),
                        log.getTimestamp() != null ? DATE_FORMATTER.format(log.getTimestamp()) : "",
                        escapeCsv(log.getIpAddress()),
                        escapeCsv(log.getUserAgent()),
                        escapeCsv(log.getCorrelationId()),
                        escapeCsv(log.getSessionId()),
                        escapeCsv(log.getWorkflowId()),
                        escapeCsv(log.getExecutionId()),
                        escapeCsv(log.getEndpoint()),
                        escapeCsv(log.getHttpMethod()),
                        log.isSuccess(),
                        escapeCsv(log.getFailureReason()),
                        log.getDurationMs()
                ));
            }
            writer.flush();
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return value.replace("\"", "\"\"");
        }
        return value;
    }

    private Sort.Direction getSortDirection(String direction) {
        if (direction == null) return Sort.Direction.DESC;
        try {
            return Sort.Direction.fromString(direction.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Sort.Direction.DESC;
        }
    }

    private ComplianceReportResponse mapToComplianceReportResponse(ComplianceReport report) {
        return ComplianceReportResponse.builder()
                .id(report.getId())
                .reportType(report.getReportType())
                .title(report.getTitle())
                .description(report.getDescription())
                .parameters(report.getParameters())
                .summary(report.getSummary())
                .status(report.getStatus())
                .format(report.getFormat())
                .recordCount(report.getRecordCount())
                .generatedBy(report.getGeneratedBy())
                .from(report.getFrom())
                .to(report.getTo())
                .generatedAt(report.getGeneratedAt())
                .createdAt(report.getCreatedAt())
                .downloadUrl(report.getDownloadUrl())
                .build();
    }
}
