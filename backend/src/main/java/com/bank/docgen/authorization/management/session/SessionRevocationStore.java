package com.bank.docgen.authorization.management.session;

import java.time.Instant;

/**
 * Revocation list for management access tokens (LR-B6): logout and renewal write the old
 * {@code jti} here, and {@code JwtAuthenticationFilter} checks it on every bearer request.
 *
 * <p>Implementations must fail loudly with {@link SessionRevocationUnavailableException} when the
 * backing store cannot be reached — callers treat that as fail-closed (reject the token).</p>
 */
public interface SessionRevocationStore {

    /**
     * Marks a token id as revoked until the token's own expiry (entries never need to
     * outlive the token, so implementations may evict at {@code expiresAt}).
     */
    void revoke(String jti, Instant expiresAt);

    boolean isRevoked(String jti);
}
