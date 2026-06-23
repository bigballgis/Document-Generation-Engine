package com.bank.docgen.runtime.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryIdempotencyCacheTest {

    private final InMemoryIdempotencyCache cache = new InMemoryIdempotencyCache();

    @Test
    void rememberAndFindWithinTtl() {
        Instant expiresAt = Instant.now().plusSeconds(60);
        cache.remember("tpl:key-1", "hash-a", expiresAt);

        assertEquals("hash-a", cache.findRequestHash("tpl:key-1").orElseThrow());
    }

    @Test
    void findReturnsEmptyAfterExpiry() {
        cache.remember("tpl:key-2", "hash-b", Instant.now().minusSeconds(1));

        assertTrue(cache.findRequestHash("tpl:key-2").isEmpty());
    }
}
