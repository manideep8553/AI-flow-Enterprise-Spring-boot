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

    public static final String LOG_EXECUTION_STARTED = "EXECUTION_STARTED";
    public static final String LOG_EXECUTION_COMPLETED = "EXECUTION_COMPLETED";
    public static final String LOG_EXECUTION_FAILED = "EXECUTION_FAILED";
    public static final String LOG_EXECUTION_CANCELLED = "EXECUTION_CANCELLED";
    public static final String LOG_TASK_CREATED = "TASK_CREATED";
    public static final String LOG_TASK_COMPLETED = "TASK_COMPLETED";
    public static final String LOG_USER_CREATED = "USER_CREATED";
    public static final String LOG_WORKFLOW_CREATED = "WORKFLOW_CREATED";
}
