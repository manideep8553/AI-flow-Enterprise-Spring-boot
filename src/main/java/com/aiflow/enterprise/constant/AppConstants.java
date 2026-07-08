package com.aiflow.enterprise.constant;

public final class AppConstants {

    private AppConstants() {
        throw new UnsupportedOperationException("Constants class cannot be instantiated");
    }

    public static final String API_BASE_PATH = "/api/v1";
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final int MAX_PAGE_SIZE = 100;

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final String SYSTEM_USER = "system";

    public static final String HASH_ALGORITHM = "SHA-256";
    public static final int HASH_ITERATIONS = 10000;

    // Execution log constants
    public static final String LOG_EXECUTION_STARTED = "EXECUTION_STARTED";
    public static final String LOG_EXECUTION_COMPLETED = "EXECUTION_COMPLETED";
    public static final String LOG_EXECUTION_FAILED = "EXECUTION_FAILED";
    public static final String LOG_EXECUTION_CANCELLED = "EXECUTION_CANCELLED";
    public static final String LOG_TASK_CREATED = "TASK_CREATED";
    public static final String LOG_TASK_COMPLETED = "TASK_COMPLETED";
    public static final String LOG_USER_CREATED = "USER_CREATED";
    public static final String LOG_WORKFLOW_CREATED = "WORKFLOW_CREATED";

    // Audit & compliance constants
    public static final String AUDIT_COLLECTION = "audit_logs";
    public static final String COMPLIANCE_COLLECTION = "compliance_reports";
    public static final String LOGIN_AUDIT_COLLECTION = "login_audits";
    public static final String AI_DECISION_LOG_COLLECTION = "ai_decision_logs";

    public static final String EXPORT_FORMAT_CSV = "csv";
    public static final String EXPORT_FORMAT_JSON = "json";

    public static final String REPORT_TYPE_USER_ACTIVITY = "USER_ACTIVITY";
    public static final String REPORT_TYPE_SECURITY_AUDIT = "SECURITY_AUDIT";
    public static final String REPORT_TYPE_WORKFLOW_AUDIT = "WORKFLOW_AUDIT";
    public static final String REPORT_TYPE_DATA_ACCESS = "DATA_ACCESS";
    public static final String REPORT_TYPE_ACTION_SUMMARY = "ACTION_SUMMARY";
    public static final String REPORT_TYPE_COMPLIANCE_SUMMARY = "COMPLIANCE_SUMMARY";

    public static final String AUDIT_STATUS_COMPLETED = "COMPLETED";
    public static final String AUDIT_STATUS_FAILED = "FAILED";
    public static final String AUDIT_STATUS_PENDING = "PENDING";
    public static final String AUDIT_STATUS_PROCESSING = "PROCESSING";

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String SESSION_ID_HEADER = "X-Session-ID";
    public static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    public static final String USER_AGENT_HEADER = "User-Agent";

    public static final int MAX_AUDIT_DETAIL_SIZE = 10000;
    public static final int DEFAULT_EXPORT_BATCH_SIZE = 5000;
    public static final int MAX_EXPORT_RECORDS = 100000;
}
