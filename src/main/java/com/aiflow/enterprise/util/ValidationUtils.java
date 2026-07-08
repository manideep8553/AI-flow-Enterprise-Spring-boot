package com.aiflow.enterprise.util;

import java.util.regex.Pattern;

public final class ValidationUtils {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_-]{3,50}$");
    private static final Pattern ALPHANUMERIC_PATTERN =
            Pattern.compile("^[a-zA-Z0-9\\s-_]+$");

    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isAlphanumeric(String value) {
        return value != null && ALPHANUMERIC_PATTERN.matcher(value).matches();
    }

    public static boolean isValidPageNumber(int page) {
        return page >= 0;
    }

    public static boolean isValidPageSize(int size) {
        return size > 0 && size <= 100;
    }
}
