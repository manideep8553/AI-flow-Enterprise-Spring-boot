package com.aiflow.enterprise.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsResponse {

    private long total;
    private long sent;
    private long delivered;
    private long failed;
    private long pending;
    private long retrying;
    private long unread;
    private Map<String, Long> byType;
    private Map<String, Long> byChannel;
    private long sentToday;
    private long sentThisWeek;
}
