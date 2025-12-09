package com.banking.account.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Placeholder configuration to allow toggling cache metrics via properties.
 * Spring Boot will auto-bind cache metrics when `management.metrics.enable.cache=true`
 * and a cache manager is present. No explicit beans are required here.
 */
@Configuration
@ConditionalOnProperty(prefix = "account.cache.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheMetricsConfig {
}

