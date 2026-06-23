package com.bank.docgen.runtime.service;

import java.time.Instant;
import java.util.Optional;

public interface IdempotencyCachePort {

    Optional<String> findRequestHash(String cacheKey);

    void remember(String cacheKey, String requestHash, Instant expiresAt);
}
