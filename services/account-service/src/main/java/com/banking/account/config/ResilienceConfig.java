package com.banking.account.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
public class ResilienceConfig {

    @Bean
    public CircuitBreakerConfig customerServiceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // Open circuit after 50% failure rate
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before trying again
                .slidingWindowSize(10) // Last 10 calls
                .minimumNumberOfCalls(5) // Need at least 5 calls before calculating failure rate
                .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 calls in half-open state
                .build();
    }

    @Bean
    public RetryConfig kafkaPublishRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(Exception.class)
                .build();
    }

    @Bean
    public RetryConfig customerServiceRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(Exception.class)
                .build();
    }
}

