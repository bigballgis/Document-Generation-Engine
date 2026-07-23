package com.bank.docgen.runtime.security;

import com.bank.docgen.infrastructure.config.RuntimeRateLimitProperties;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/**
 * Redis-coordinated runtime rate limiter (PQH-F7 / ADR-0044).
 *
 * <p>Uses Lettuce via {@link StringRedisTemplate} and a Lua token-bucket script that preserves
 * Bucket4j capacity + greedy refill semantics. {@code bucket4j-redis} is not on the approved
 * classpath (only {@code bucket4j-core}); Redisson locks remain out of scope (ADR-0039).
 */
public class RedisRuntimeRateLimitService implements RuntimeRateLimiter {

    private static final Logger LOG = LoggerFactory.getLogger(RedisRuntimeRateLimitService.class);

    static final String KEY_PREFIX = "docgen:runtime:rate-limit:";

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final DefaultRedisScript<List> CONSUME_SCRIPT = new DefaultRedisScript<>();

    static {
        CONSUME_SCRIPT.setResultType(List.class);
        CONSUME_SCRIPT.setScriptText(
                """
                local capacity = tonumber(ARGV[1])
                local refill_per_min = tonumber(ARGV[2])
                local now = tonumber(ARGV[3])
                local requested = tonumber(ARGV[4])

                local data = redis.call('HMGET', KEYS[1], 'tokens', 'updated_at')
                local tokens = tonumber(data[1])
                local updated_at = tonumber(data[2])

                if tokens == nil then
                  tokens = capacity
                  updated_at = now
                end

                if now > updated_at and refill_per_min > 0 then
                  local refill = ((now - updated_at) / 60000.0) * refill_per_min
                  tokens = math.min(capacity, tokens + refill)
                  updated_at = now
                end

                local consumed = 0
                local nanos_to_wait = 0
                if tokens >= requested then
                  tokens = tokens - requested
                  consumed = 1
                else
                  local deficit = requested - tokens
                  if refill_per_min > 0 then
                    nanos_to_wait = math.ceil((deficit / refill_per_min) * 60 * 1000000000)
                  else
                    nanos_to_wait = 60000000000
                  end
                end

                redis.call('HMSET', KEYS[1], 'tokens', tokens, 'updated_at', updated_at)
                redis.call('PEXPIRE', KEYS[1], 120000)
                return {consumed, math.floor(tokens), nanos_to_wait}
                """
        );
    }

    private final RuntimeRateLimitProperties properties;
    private final StringRedisTemplate redisTemplate;

    public RedisRuntimeRateLimitService(
            RuntimeRateLimitProperties properties,
            StringRedisTemplate redisTemplate
    ) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean enabled() {
        return properties.enabled();
    }

    @Override
    public RateLimitDecision tryConsumeKey(String key) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> result = redisTemplate.execute(
                    CONSUME_SCRIPT,
                    Collections.singletonList(KEY_PREFIX + key),
                    Long.toString(properties.burstCapacity()),
                    Long.toString(properties.requestsPerMinute()),
                    Long.toString(System.currentTimeMillis()),
                    "1"
            );
            if (result == null || result.size() < 3) {
                LOG.warn("Rate-limit Redis script returned unexpected result for key hash");
                return RateLimitDecision.backendUnavailable();
            }
            long consumed = toLong(result.get(0));
            long nanosToWait = toLong(result.get(2));
            if (consumed == 1L) {
                return RateLimitDecision.allowed();
            }
            return RateLimitDecision.denied(nanosToWait);
        } catch (RuntimeException ex) {
            LOG.warn("Rate-limit Redis coordination unavailable: {}", ex.getClass().getSimpleName());
            return RateLimitDecision.backendUnavailable();
        }
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(value.toString());
    }
}
