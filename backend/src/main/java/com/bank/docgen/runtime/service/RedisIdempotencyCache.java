package com.bank.docgen.runtime.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisIdempotencyCache implements IdempotencyCachePort {

    private final StringRedisTemplate redisTemplate;
    private final String keyPrefix;

    public RedisIdempotencyCache(StringRedisTemplate redisTemplate, String keyPrefix) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public Optional<String> findRequestHash(String cacheKey) {
        String value = redisTemplate.opsForValue().get(prefixedKey(cacheKey));
        return Optional.ofNullable(value);
    }

    @Override
    public void remember(String cacheKey, String requestHash, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redisTemplate.opsForValue().set(prefixedKey(cacheKey), requestHash, ttl);
    }

    private String prefixedKey(String cacheKey) {
        return keyPrefix + cacheKey;
    }
}
