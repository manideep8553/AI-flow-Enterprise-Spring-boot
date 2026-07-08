package com.aiflow.enterprise.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public final class AppUtils {

    private static final Logger log = LoggerFactory.getLogger(AppUtils.class);

    private AppUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    public static String generateStepId() {
        return "step-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isBlank();
    }

    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNullOrEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static <T> T defaultIfNull(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static void logMethodEntry(Logger logger, String methodName, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug("Entering {} with args: {}", methodName, args);
        }
    }

    public static void logMethodExit(Logger logger, String methodName, Object result) {
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting {} with result: {}", methodName, result);
        }
    }

    public static String sanitize(String input) {
        if (input == null) return null;
        return input.trim().replaceAll("[<>]", "");
    }
}
