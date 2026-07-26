package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * FOS-W13-6 — Redis idempotency cache against Testcontainers Redis (production sibling).
 */
@Tag("testcontainers")
@Testcontainers
class RedisIdempotencyCacheTestcontainersTest {

    private static final String REDIS_IMAGE = "redis:7-alpine";

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse(REDIS_IMAGE))
            .withExposedPorts(6379);

    private RedisIdempotencyCache cache;

    @BeforeEach
    void setUp() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        factory.afterPropertiesSet();
        StringRedisTemplate template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
        cache = new RedisIdempotencyCache(template, "docgen:idem:w13:");
    }

    @Test
    void rememberAndFindRoundTripOnRealRedis() {
        String key = "req-w13-1";
        cache.remember(key, "hash-a", Instant.now().plus(5, ChronoUnit.MINUTES));
        assertThat(cache.findRequestHash(key)).contains("hash-a");
    }
}
