package com.banking.card.config;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CacheMetricsRegistrar;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "card.cache.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheMetricsConfig {

    @Bean
    MeterBinder cacheMetricsBinder(CacheManager cacheManager) {
        return registry -> cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (Objects.nonNull(cache)) {
                CacheMetricsRegistrar registrar = new CacheMetricsRegistrar(registry, Tag.of("cache", cacheName));
                registrar.bindCacheToRegistry(cache);
            }
        });
    }
}

