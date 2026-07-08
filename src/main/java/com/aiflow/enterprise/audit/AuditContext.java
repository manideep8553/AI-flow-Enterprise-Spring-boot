package com.aiflow.enterprise.audit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AuditContext {

    private static final ThreadLocal<AuditContext> CONTEXT_HOLDER = ThreadLocal.withInitial(AuditContext::new);

    private final String correlationId;
    private String performedBy;
    private String ipAddress;
    private String userAgent;
    private String sessionId;
    private String requestId;
    private Map<String, Object> metadata;

    private AuditContext() {
        this.correlationId = UUID.randomUUID().toString();
        this.metadata = new HashMap<>();
    }

    public static AuditContext get() {
        return CONTEXT_HOLDER.get();
    }

    public static void setCurrent(AuditContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static void clear() {
        CONTEXT_HOLDER.remove();
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }
}
