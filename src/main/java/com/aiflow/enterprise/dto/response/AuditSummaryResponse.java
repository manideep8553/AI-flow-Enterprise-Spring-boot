package com.aiflow.enterprise.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditSummaryResponse {

    private long totalLogs;
    private long successCount;
    private long failureCount;
    private long uniqueUsers;
    private long uniqueActions;
    private Map<String, Long> actionBreakdown;
    private Map<String, Long> entityTypeBreakdown;
    private Map<String, Long> userActivityTop;
    private List<TimeSeriesPoint> timeSeries;
    private Instant from;
    private Instant to;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TimeSeriesPoint {
        private String period;
        private long count;
        private long successCount;
        private long failureCount;
    }
}
