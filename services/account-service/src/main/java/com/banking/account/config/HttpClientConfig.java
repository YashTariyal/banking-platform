package com.banking.account.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Applies safe client timeouts so outbound calls fail fast and rely on
 * Resilience4j for retries/circuit breaking rather than hanging sockets.
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestTemplate restTemplate(
            RestTemplateBuilder builder,
            @Value("${account.http.client.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${account.http.client.read-timeout-ms:5000}") int readTimeoutMs) {
        return builder
                .setConnectTimeout(Duration.ofMillis(connectTimeoutMs))
                .setReadTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }
}
