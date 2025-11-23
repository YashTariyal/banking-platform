package com.banking.account.config;

import com.banking.account.domain.Account;
import com.banking.account.web.dto.AccountResponse;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String ACCOUNT_CACHE = "accounts";
    public static final String ACCOUNT_BY_NUMBER_CACHE = "accounts-by-number";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // Default TTL: 10 minutes
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Account cache configuration
        RedisCacheConfiguration accountCacheConfig = defaultConfig.entryTtl(Duration.ofMinutes(15));

        // Account by number cache configuration
        RedisCacheConfiguration accountByNumberCacheConfig = defaultConfig.entryTtl(Duration.ofMinutes(15));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration(ACCOUNT_CACHE, accountCacheConfig)
                .withCacheConfiguration(ACCOUNT_BY_NUMBER_CACHE, accountByNumberCacheConfig)
                .transactionAware()
                .build();
    }
}

