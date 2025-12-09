package com.banking.account.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;

class CacheMetricsConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheConfig.class, CacheMetricsConfig.class))
            .withPropertyValues(
                    "account.cache.metrics.enabled=true",
                    "management.metrics.enable.cache=true");

    @Test
    void enablesCachingAndCacheManagerPresent() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CacheManager.class);
            assertThat(context).hasSingleBean(CacheMetricsConfig.class);
        });
    }
}

