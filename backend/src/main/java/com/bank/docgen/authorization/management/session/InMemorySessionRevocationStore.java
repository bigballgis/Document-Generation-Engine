package com.bank.docgen.authorization.management.session;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transitional-test-only revocation store (test profile / explicit
 * {@code docgen.session.revocation-store=memory}). Semantics match the Redis store
 * (revocation hit until token expiry, lazy eviction) but state does NOT survive restarts,
 * so it is forbidden in prod (LR-B6 Do-NOT; guarded in {@code SessionRevocationConfig}).
 */
public class InMemorySessionRevocationStore implements SessionRevocationStore {

    private final Map<String, Instant> revokedUntil = new ConcurrentHashMap<>();

    @Override
    public void revoke(String jti, Instant expiresAt) {
        // Past-dated entries are stored anyway and lazily evicted on read — observable
        // behavior (isRevoked=false) matches the Redis store skipping the expired write.
        revokedUntil.put(jti, expiresAt);
    }

    @Override
    public boolean isRevoked(String jti) {
        Instant expiresAt = revokedUntil.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt.isBefore(Instant.now())) {
            revokedUntil.remove(jti);
            return false;
        }
        return true;
    }
}
