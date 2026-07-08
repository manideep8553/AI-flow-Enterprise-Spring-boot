package com.aiflow.enterprise.security;

import com.aiflow.enterprise.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxRequestsPerMinute;

    public RateLimitingFilter(StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper,
                              @Value("${app.rate-limiting.max-requests-per-minute:60}") int maxRequestsPerMinute) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String key = "rate_limit:" + clientIp;

        if (isRateLimited(key)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            ApiResponse<Void> apiResponse = ApiResponse.error(
                    "Too many requests. Please try again later.",
                    HttpStatus.TOO_MANY_REQUESTS);
            objectMapper.writeValue(response.getOutputStream(), apiResponse);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String key) {
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, 1, TimeUnit.MINUTES);
            }
            return count != null && count > maxRequestsPerMinute;
        } catch (Exception e) {
            log.warn("Redis rate limiting unavailable, allowing request: {}", e.getMessage());
            return false;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/health") || path.startsWith("/actuator/info");
    }
}
