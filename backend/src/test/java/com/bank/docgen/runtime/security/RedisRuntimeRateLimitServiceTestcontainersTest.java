package com.bank.docgen.runtime.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.infrastructure.config.RuntimeRateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * FOS-W13-6 — coordinated Redis rate-limit against Testcontainers Redis.
 */
@Tag("testcontainers")
@Testcontainers
class RedisRuntimeRateLimitServiceTestcontainersTest {

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    private RedisRuntimeRateLimitService service;

    @BeforeEach
    void setUp() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        RuntimeRateLimitProperties properties = new RuntimeRateLimitProperties(true, 120, 120, true);
        service = new RedisRuntimeRateLimitService(properties, template);
    }

    @Test
    void tryConsumeKeyAllowsUnderBurstOnRealRedis() {
        assertThat(service.enabled()).isTrue();
        RateLimitDecision decision = service.tryConsumeKey("cred-a:acct-a");
        assertThat(decision).isNotNull();
        assertThat(decision.isAllowed()).isTrue();
    }
}
