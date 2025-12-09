package com.banking.card.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

class CacheMetricsConfigTest {

    @Test
    void bindsCachesToMeterRegistry() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("cards", "cardAnalytics");
        CacheMetricsConfig config = new CacheMetricsConfig();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

        config.cacheMetricsBinder(cacheManager).bindTo(meterRegistry);

        assertThat(meterRegistry.find("cache.gets").tags("cache", "cards").meter()).isPresent();
        assertThat(meterRegistry.find("cache.gets").tags("cache", "cardAnalytics").meter()).isPresent();
    }
}

