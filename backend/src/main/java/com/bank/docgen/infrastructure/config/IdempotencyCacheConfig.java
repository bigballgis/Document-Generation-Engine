package com.bank.docgen.infrastructure.config;

import com.bank.docgen.runtime.service.IdempotencyCachePort;
import com.bank.docgen.runtime.service.InMemoryIdempotencyCache;
import com.bank.docgen.runtime.service.RedisIdempotencyCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class IdempotencyCacheConfig {

    @Bean
    @Profile("test")
    public IdempotencyCachePort testIdempotencyCachePort() {
        return new InMemoryIdempotencyCache();
    }

    @Bean
    @Profile("!test")
    @ConditionalOnProperty(name = "docgen.idempotency.cache", havingValue = "redis", matchIfMissing = true)
    public IdempotencyCachePort redisIdempotencyCachePort(
            StringRedisTemplate redisTemplate,
            DocgenIdempotencyProperties properties
    ) {
        return new RedisIdempotencyCache(redisTemplate, properties.getRedisKeyPrefix());
    }

    @Bean
    @Profile("!test")
    @ConditionalOnProperty(name = "docgen.idempotency.cache", havingValue = "memory")
    public IdempotencyCachePort memoryIdempotencyCachePort() {
        return new InMemoryIdempotencyCache();
    }
}
