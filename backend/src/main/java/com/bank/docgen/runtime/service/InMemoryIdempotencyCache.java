package com.bank.docgen.runtime.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryIdempotencyCache implements IdempotencyCachePort {

    private record CacheEntry(String requestHash, Instant expiresAt) {
    }

    private final Map<String, CacheEntry> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<String> findRequestHash(String cacheKey) {
        CacheEntry entry = entries.get(cacheKey);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            entries.remove(cacheKey);
            return Optional.empty();
        }
        return Optional.of(entry.requestHash());
    }

    @Override
    public void remember(String cacheKey, String requestHash, Instant expiresAt) {
        entries.put(cacheKey, new CacheEntry(requestHash, expiresAt));
    }
}
