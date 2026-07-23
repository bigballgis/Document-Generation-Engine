package com.bank.docgen.infrastructure.config;

import com.bank.docgen.runtime.security.RedisRuntimeRateLimitService;
import com.bank.docgen.runtime.security.RuntimeRateLimitService;
import com.bank.docgen.runtime.security.RuntimeRateLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Selects process-local vs Redis-coordinated runtime rate limiting (PQH-F7).
 *
 * <p>Default {@code docgen.runtime.rate-limit.distributed=false} keeps in-process Bucket4j.
 * Opt-in {@code true} wires {@link RedisRuntimeRateLimitService} (Lettuce token-bucket Lua).
 */
@Configuration
public class RuntimeRateLimitConfig {

    @Bean
    @ConditionalOnProperty(
            prefix = "docgen.runtime.rate-limit",
            name = "distributed",
            havingValue = "false",
            matchIfMissing = true
    )
    public RuntimeRateLimiter processLocalRuntimeRateLimiter(RuntimeRateLimitProperties properties) {
        return new RuntimeRateLimitService(properties);
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "docgen.runtime.rate-limit",
            name = "distributed",
            havingValue = "true"
    )
    public RuntimeRateLimiter redisRuntimeRateLimiter(
            RuntimeRateLimitProperties properties,
            StringRedisTemplate redisTemplate
    ) {
        return new RedisRuntimeRateLimitService(properties, redisTemplate);
    }
}
