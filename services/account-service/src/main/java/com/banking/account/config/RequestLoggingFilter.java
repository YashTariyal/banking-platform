package com.banking.account.config;

import com.banking.account.metrics.AccountMetrics;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Filter for logging HTTP requests and responses for audit purposes.
 * Adds correlation ID for request tracing, PII masking, and metrics collection.
 */
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC = "correlationId";

    private final PiiMaskingFilter piiMaskingFilter;
    private final AccountMetrics accountMetrics;

    public RequestLoggingFilter(PiiMaskingFilter piiMaskingFilter, AccountMetrics accountMetrics) {
        this.piiMaskingFilter = piiMaskingFilter;
        this.accountMetrics = accountMetrics;
    }

    @Override
    protected void doFilterInternal(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = getOrCreateCorrelationId(request);
        MDC.put(CORRELATION_ID_MDC, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        long startTime = System.currentTimeMillis();
        Timer.Sample timerSample = accountMetrics.startApiTimer();

        // Wrap request/response to enable reading body multiple times
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String method = request.getMethod();
            String path = request.getRequestURI();
            int status = response.getStatus();

            // Record metrics
            accountMetrics.recordApiTime(timerSample, method, path, status);
            if (status >= 400) {
                accountMetrics.recordApiError(method, path, status);
            }

            // Log request/response
            logRequest(wrappedRequest, wrappedResponse, duration, status);
            wrappedResponse.copyBodyToResponse();
            MDC.clear();
        }
    }

    private String getOrCreateCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        return correlationId;
    }

    private void logRequest(
            ContentCachingRequestWrapper request,
            ContentCachingResponseWrapper response,
            long duration,
            int status) {
        
        // Only log non-actuator endpoints to reduce noise
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/api-docs")) {
            return;
        }

        String method = request.getMethod();
        String clientIp = getClientIpAddress(request);
        
        // Log request body (masked)
        String requestBody = getRequestBody(request);
        String maskedRequestBody = piiMaskingFilter.maskPii(requestBody);
        
        // Log response body (masked) for errors
        String responseBody = status >= 400 ? getResponseBody(response) : null;
        String maskedResponseBody = responseBody != null ? piiMaskingFilter.maskPii(responseBody) : null;

        if (maskedResponseBody != null) {
            log.info("HTTP {} {} - Status: {} - Duration: {}ms - IP: {} - Request: {} - Response: {}",
                    method, path, status, duration, clientIp, maskedRequestBody, maskedResponseBody);
        } else {
            log.info("HTTP {} {} - Status: {} - Duration: {}ms - IP: {} - Request: {}",
                    method, path, status, duration, clientIp, maskedRequestBody);
        }
    }

    private String getRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return "";
    }

    private String getResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            return new String(content, StandardCharsets.UTF_8);
        }
        return "";
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

