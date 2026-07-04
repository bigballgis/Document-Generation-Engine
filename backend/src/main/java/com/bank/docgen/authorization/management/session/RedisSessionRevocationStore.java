package com.bank.docgen.authorization.management.session;

import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Default revocation store: Redis keys {@code docgen:session:revoked:<jti>} with
 * TTL = remaining token life, so the list stays bounded and survives instance restarts.
 */
public class RedisSessionRevocationStore implements SessionRevocationStore {

    static final String KEY_PREFIX = "docgen:session:revoked:";
    private static final String REVOKED_MARKER = "1";

    private final StringRedisTemplate redisTemplate;

    public RedisSessionRevocationStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void revoke(String jti, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + jti, REVOKED_MARKER, ttl);
        } catch (RuntimeException ex) {
            throw new SessionRevocationUnavailableException(ex);
        }
    }

    @Override
    public boolean isRevoked(String jti) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
        } catch (RuntimeException ex) {
            throw new SessionRevocationUnavailableException(ex);
        }
    }
}
