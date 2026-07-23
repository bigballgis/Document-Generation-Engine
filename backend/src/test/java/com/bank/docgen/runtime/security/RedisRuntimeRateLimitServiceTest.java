package com.bank.docgen.runtime.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.config.RuntimeRateLimitProperties;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * BDD-PQH-F7-002 / 006 / 009 / 010 — Redis-coordinated limiter (Lettuce custom token bucket).
 */
class RedisRuntimeRateLimitServiceTest {

    @Test
    void sharedQuotaAcrossTwoLogicalClientsDoesNotExceedBurst() {
        SharedHashRedis fakeRedis = new SharedHashRedis();
        RuntimeRateLimitProperties properties = new RuntimeRateLimitProperties(true, 60, 3, true);
        RedisRuntimeRateLimitService instanceA = new RedisRuntimeRateLimitService(properties, fakeRedis.template());
        RedisRuntimeRateLimitService instanceB = new RedisRuntimeRateLimitService(properties, fakeRedis.template());

        int allowed = 0;
        for (int i = 0; i < 3; i++) {
            if (instanceA.tryConsume("CRED-1", "acct-1").isAllowed()) {
                allowed++;
            }
            if (instanceB.tryConsume("CRED-1", "acct-1").isAllowed()) {
                allowed++;
            }
        }

        assertThat(allowed).isEqualTo(3);
        assertThat(instanceA.tryConsume("CRED-1", "acct-1").isDenied()).isTrue();
        assertThat(instanceB.tryConsume("CRED-1", "acct-1").isDenied()).isTrue();
    }

    @Test
    void redisFailureReturnsBackendUnavailableNotDenied() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("redis down"));

        RedisRuntimeRateLimitService service = new RedisRuntimeRateLimitService(
                new RuntimeRateLimitProperties(true, 60, 3, true),
                redisTemplate
        );

        RateLimitDecision decision = service.tryConsumeKey("CRED-1:acct-1");

        assertThat(decision.isBackendUnavailable()).isTrue();
        assertThat(decision.isDenied()).isFalse();
        assertThat(decision.isAllowed()).isFalse();
    }

    @Test
    void underQuotaAllowsWhenRedisReturnsConsumed() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(1L, 2L, 0L));

        RedisRuntimeRateLimitService service = new RedisRuntimeRateLimitService(
                new RuntimeRateLimitProperties(true, 60, 3, true),
                redisTemplate
        );

        assertThat(service.tryConsumeKey("CRED-1:acct-1").isAllowed()).isTrue();
    }

    @Test
    void exhaustedQuotaDeniesWithWaitNanos() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(0L, 0L, 1_500_000_000L));

        RedisRuntimeRateLimitService service = new RedisRuntimeRateLimitService(
                new RuntimeRateLimitProperties(true, 60, 1, true),
                redisTemplate
        );

        RateLimitDecision decision = service.tryConsumeKey("CRED-1:acct-1");
        assertThat(decision.isDenied()).isTrue();
        assertThat(decision.nanosToWaitForRefill()).isEqualTo(1_500_000_000L);
    }

    /**
     * Minimal shared Redis stand-in: runs the same greedy token-bucket math the Lua script encodes,
     * so two service instances prove shared-quota semantics without a live Redis.
     */
    private static final class SharedHashRedis {
        private final Map<String, BucketState> buckets = new ConcurrentHashMap<>();
        private final AtomicLong clockMs = new AtomicLong(System.currentTimeMillis());
        private final StringRedisTemplate template = mock(StringRedisTemplate.class);

        SharedHashRedis() {
            when(template.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString(), anyString()))
                    .thenAnswer(invocation -> {
                        @SuppressWarnings("unchecked")
                        List<String> keys = invocation.getArgument(1);
                        String capacity = invocation.getArgument(2);
                        String rpm = invocation.getArgument(3);
                        String nowMs = invocation.getArgument(4);
                        return tryConsume(
                                keys.getFirst(),
                                Long.parseLong(capacity),
                                Long.parseLong(rpm),
                                Long.parseLong(nowMs)
                        );
                    });
        }

        StringRedisTemplate template() {
            return template;
        }

        private List<Long> tryConsume(String key, long capacity, long rpm, long nowMs) {
            clockMs.set(nowMs);
            BucketState state = buckets.computeIfAbsent(key, ignored -> new BucketState(capacity, nowMs));
            synchronized (state) {
                double tokens = state.tokens;
                long updatedAt = state.updatedAtMs;
                if (nowMs > updatedAt && rpm > 0) {
                    double refill = ((nowMs - updatedAt) / 60_000.0d) * rpm;
                    tokens = Math.min(capacity, tokens + refill);
                    updatedAt = nowMs;
                }
                if (tokens >= 1.0d) {
                    tokens -= 1.0d;
                    state.tokens = tokens;
                    state.updatedAtMs = updatedAt;
                    return List.of(1L, (long) Math.floor(tokens), 0L);
                }
                long nanosToWait = 60_000_000_000L;
                if (rpm > 0) {
                    double deficit = 1.0d - tokens;
                    nanosToWait = (long) Math.ceil((deficit / rpm) * 60.0d * 1_000_000_000.0d);
                }
                state.tokens = tokens;
                state.updatedAtMs = updatedAt;
                return List.of(0L, (long) Math.floor(tokens), nanosToWait);
            }
        }

        private static final class BucketState {
            private double tokens;
            private long updatedAtMs;

            private BucketState(long capacity, long nowMs) {
                this.tokens = capacity;
                this.updatedAtMs = nowMs;
            }
        }
    }
}
