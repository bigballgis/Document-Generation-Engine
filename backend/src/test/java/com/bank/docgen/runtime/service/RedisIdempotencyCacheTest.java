package com.bank.docgen.runtime.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisIdempotencyCacheTest {

    private static final String KEY_PREFIX = "docgen:idempotency:";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisIdempotencyCache cache;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cache = new RedisIdempotencyCache(redisTemplate, KEY_PREFIX);
    }

    @Test
    void findRequestHashReturnsStoredValue() {
        when(valueOperations.get(KEY_PREFIX + "tpl:key-1")).thenReturn("abc123");

        Optional<String> hash = cache.findRequestHash("tpl:key-1");

        assertTrue(hash.isPresent());
        assertEquals("abc123", hash.get());
    }

    @Test
    void rememberStoresValueWithTtl() {
        Instant expiresAt = Instant.now().plusSeconds(3600);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

        cache.remember("tpl:key-2", "def456", expiresAt);

        verify(valueOperations).set(
                eq(KEY_PREFIX + "tpl:key-2"),
                eq("def456"),
                ttlCaptor.capture()
        );
        assertTrue(ttlCaptor.getValue().toSeconds() > 3500);
    }

    @Test
    void rememberSkipsExpiredEntries() {
        cache.remember("tpl:key-3", "ghi789", Instant.now().minusSeconds(1));

        verify(valueOperations, never()).set(
                org.mockito.ArgumentMatchers.eq(KEY_PREFIX + "tpl:key-3"),
                org.mockito.ArgumentMatchers.eq("ghi789"),
                org.mockito.ArgumentMatchers.any(Duration.class)
        );
    }
}
