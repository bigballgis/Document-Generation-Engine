package com.bank.docgen.infrastructure.config;

import com.bank.docgen.authorization.management.session.InMemorySessionRevocationStore;
import com.bank.docgen.authorization.management.session.RedisSessionRevocationStore;
import com.bank.docgen.authorization.management.session.SessionRevocationStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Selects the LR-B6 session revocation store, mirroring {@link IdempotencyCacheConfig}:
 * Redis by default, in-memory only for the test profile or an explicit non-prod opt-in.
 */
@Configuration
public class SessionRevocationConfig {

    @Bean
    @Profile("test")
    public SessionRevocationStore testSessionRevocationStore() {
        // Transitional-test-only: same TTL/hit semantics as Redis, but not restart-durable.
        return new InMemorySessionRevocationStore();
    }

    @Bean
    @Profile("!test")
    @ConditionalOnProperty(name = "docgen.session.revocation-store", havingValue = "redis", matchIfMissing = true)
    public SessionRevocationStore redisSessionRevocationStore(StringRedisTemplate redisTemplate) {
        return new RedisSessionRevocationStore(redisTemplate);
    }

    @Bean
    @Profile("!test")
    @ConditionalOnProperty(name = "docgen.session.revocation-store", havingValue = "memory")
    public SessionRevocationStore memorySessionRevocationStore(
            @Value("${docgen.environment}") String environment) {
        if ("prod".equalsIgnoreCase(environment)) {
            // LR-B6 Do-NOT: revocation must survive instance restarts in prod (spec B7 guard).
            throw new IllegalStateException(
                    "docgen.session.revocation-store=memory is forbidden when docgen.environment=prod");
        }
        return new InMemorySessionRevocationStore();
    }
}
