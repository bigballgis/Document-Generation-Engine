package com.bank.docgen.sharedkernel.security;

import com.bank.docgen.authorization.management.domain.AuthSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Claims carried by a management access token (LR-B6): {@code jti} identifies the token for
 * revocation, {@code sessionStartedAt} anchors the 8h absolute session limit across renewals.
 */
public record ManagementSessionClaims(
        String username,
        String displayName,
        String email,
        AuthSource authSource,
        List<String> roles,
        List<String> authorizedGroupCodes,
        String defaultRoute,
        List<String> visibleRoutes,
        String jti,
        Instant sessionStartedAt,
        Instant expiresAt
) {

    /**
     * Convenience constructor for a fresh session (new token id, session started now).
     */
    public ManagementSessionClaims(
            String username,
            String displayName,
            String email,
            AuthSource authSource,
            List<String> roles,
            List<String> authorizedGroupCodes,
            String defaultRoute,
            List<String> visibleRoutes,
            Instant expiresAt
    ) {
        this(username, displayName, email, authSource, roles, authorizedGroupCodes,
                defaultRoute, visibleRoutes, UUID.randomUUID().toString(), Instant.now(), expiresAt);
    }
}
