package com.aiflow.enterprise.audit;

import com.aiflow.enterprise.entity.AuditLog;
import com.aiflow.enterprise.enums.AuditActionType;
import com.aiflow.enterprise.repository.AuditLogRepository;
import com.aiflow.enterprise.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final AuditLogRepository auditLogRepository;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Pointcut("@annotation(auditable)")
    public void auditableMethods(Auditable auditable) {
    }

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restControllerMethods() {
    }

    @Around("auditableMethods(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        AuditContext ctx = AuditContext.get();
        String correlationId = ctx.getCorrelationId();
        MDC.put("correlationId", correlationId);

        String action = auditable.action();
        String entityType = auditable.entityType();
        boolean captureInput = auditable.captureInput();
        boolean captureOutput = auditable.captureOutput();

        Map<String, Object> inputDetails = new HashMap<>();
        if (captureInput) {
            String[] paramNames = getParameterNames(joinPoint);
            Object[] args = joinPoint.getArgs();
            for (int i = 0; i < args.length; i++) {
                if (i < paramNames.length) {
                    inputDetails.put(paramNames[i], sanitizeForAudit(args[i]));
                }
            }
        }

        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean success = true;
        String errorMessage = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            AuditActionType actionType = parseActionType(action);
            if (actionType == null) {
                actionType = AuditActionType.DATA_UPDATED;
            }

            Map<String, Object> details = new HashMap<>();
            if (!inputDetails.isEmpty()) {
                details.put("input", inputDetails);
            }
            if (captureOutput && success && result != null) {
                details.put("output", sanitizeForAudit(result));
            }
            if (errorMessage != null) {
                details.put("error", errorMessage);
            }
            details.put("durationMs", duration);
            if (ctx.getMetadata() != null && !ctx.getMetadata().isEmpty()) {
                details.putAll(ctx.getMetadata());
            }

            AuditLog auditLog = AuditLog.builder()
                    .action(actionType.name())
                    .entityType(entityType)
                    .entityId(null)
                    .performedBy(ctx.getPerformedBy())
                    .previousValues(null)
                    .newValues(null)
                    .details(details)
                    .ipAddress(ctx.getIpAddress())
                    .userAgent(ctx.getUserAgent())
                    .correlationId(correlationId)
                    .sessionId(ctx.getSessionId())
                    .requestId(ctx.getRequestId())
                    .success(success)
                    .immutable(true)
                    .timestamp(Instant.now())
                    .build();

            try {
                auditLogRepository.save(auditLog);
            } catch (Exception e) {
                log.error("Failed to persist audit log: {}", e.getMessage());
            }

            MDC.remove("correlationId");
        }
    }

    @Around("restControllerMethods()")
    public Object auditHttpRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .orElse(null);

        AuditContext ctx = AuditContext.get();

        if (request != null) {
            ctx.setIpAddress(request.getRemoteAddr());
            ctx.setUserAgent(request.getHeader("User-Agent"));
            ctx.setRequestId(request.getHeader("X-Request-ID"));

            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isEmpty()) {
                ctx.setIpAddress(forwardedFor.split(",")[0].trim());
            }

            ctx.addMetadata("endpoint", request.getRequestURI());
            ctx.addMetadata("httpMethod", request.getMethod());
            ctx.addMetadata("queryString", request.getQueryString());
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            ctx.setPerformedBy(userDetails.getId());
            ctx.addMetadata("username", userDetails.getUsername());
            ctx.addMetadata("userEmail", userDetails.getEmail());
            ctx.addMetadata("userRole", userDetails.getRole().name());
        }

        if (ctx.getPerformedBy() == null) {
            ctx.setPerformedBy("anonymous");
        }

        try {
            return joinPoint.proceed();
        } finally {
            AuditContext.clear();
        }
    }

    private AuditActionType parseActionType(String action) {
        if (action == null || action.isEmpty()) return null;
        try {
            return AuditActionType.valueOf(action.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String[] getParameterNames(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return java.util.Arrays.stream(method.getParameters())
                .map(java.lang.reflect.Parameter::getName)
                .toArray(String[]::new);
    }

    private Object sanitizeForAudit(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) {
            if (s.length() > 1000) return s.substring(0, 1000) + "... [truncated]";
            return s;
        }
        if (obj instanceof byte[]) return "[binary data]";
        if (obj.getClass().getName().contains("password") || obj.getClass().getName().contains("Password")) {
            return "[redacted]";
        }
        return obj;
    }
}
