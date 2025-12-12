package com.banking.account.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

class CacheMetricsConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheConfig.class, CacheMetricsConfig.class))
            .withBean(RedisConnectionFactory.class, () -> {
                // Provide a mock/in-memory Redis connection factory for testing
                RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6379);
                LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
                factory.afterPropertiesSet();
                return factory;
            })
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

