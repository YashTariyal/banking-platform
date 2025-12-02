package com.banking.account.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(0) // Execute before other filters
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "account.rate-limit.enabled", havingValue = "true", matchIfMissing = false)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final String RATE_LIMIT_HEADER = "X-RateLimit-Remaining";

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties rateLimitProperties;

    public RateLimitFilter(ProxyManager<String> proxyManager, RateLimitProperties rateLimitProperties) {
        this.proxyManager = proxyManager;
        this.rateLimitProperties = rateLimitProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!rateLimitProperties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for actuator endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/actuator") || path.startsWith("/swagger") || path.startsWith("/api-docs")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = getClientKey(request);
        Bucket bucket = resolveBucket(clientKey, request);

        if (bucket.tryConsume(1)) {
            long availableTokens = bucket.getAvailableTokens();
            response.setHeader(RATE_LIMIT_HEADER, String.valueOf(availableTokens));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for client: {} - Path: {}", clientKey, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60"); // Retry after 60 seconds
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
        }
    }

    private String getClientKey(HttpServletRequest request) {
        // Prefer JWT subject/client when available to enforce per-user or per-client limits.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String subject = Optional.ofNullable(jwt.getSubject()).orElse("anonymous");
            String clientId = Optional.ofNullable(jwt.getClaimAsString("client_id")).orElse(null);
            if (clientId != null && !clientId.isBlank()) {
                return "client:" + clientId + ":sub:" + subject;
            }
            return "sub:" + subject;
        }

        // Fallback: Use IP address as the key, or API key if available
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return "api-key:" + apiKey;
        }
        
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return "ip:" + xForwardedFor.split(",")[0].trim();
        }
        
        return "ip:" + request.getRemoteAddr();
    }

    private Bucket resolveBucket(String clientKey, HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // Different rate limits based on endpoint
        int requestsPerMinute;
        if (path.contains("/transactions") && "POST".equals(method)) {
            requestsPerMinute = rateLimitProperties.getTransactionRequestsPerMinute();
        } else if (path.matches(".*/accounts/?$") && "POST".equals(method)) {
            requestsPerMinute = rateLimitProperties.getCreateAccountRequestsPerMinute();
        } else if ("GET".equals(method)) {
            requestsPerMinute = rateLimitProperties.getReadRequestsPerMinute();
        } else {
            requestsPerMinute = rateLimitProperties.getDefaultRequestsPerMinute();
        }

        Bandwidth limit = Bandwidth.classic(
                requestsPerMinute,
                Refill.intervally(requestsPerMinute, Duration.ofMinutes(1))
        );

        io.github.bucket4j.BucketConfiguration configuration = io.github.bucket4j.BucketConfiguration.builder()
                .addLimit(limit)
                .build();

        return proxyManager.builder()
                .build(clientKey, configuration);
    }
}

