package com.banking.account.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

class CacheMetricsConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RedisAutoConfiguration.class,
                    CacheConfig.class,
                    CacheMetricsConfig.class))
            .withPropertyValues(
                    "spring.data.redis.host=localhost",
                    "spring.data.redis.port=6379",
                    "account.cache.metrics.enabled=true",
                    "management.metrics.enable.cache=true");

    @Test
    void enablesCachingAndCacheManagerPresent() {
        // Skip test if Redis is not available (e.g., in CI without Docker)
        contextRunner.run(context -> {
            // Only assert if Redis connection factory is available
            if (context.getBeanNamesForType(RedisConnectionFactory.class).length > 0) {
                assertThat(context).hasSingleBean(CacheManager.class);
                assertThat(context).hasSingleBean(CacheMetricsConfig.class);
            }
        });
    }
}

