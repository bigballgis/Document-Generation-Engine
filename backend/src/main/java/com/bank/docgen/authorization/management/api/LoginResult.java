package com.bank.docgen.authorization.management.api;

import java.time.Instant;

/**
 * Shared response shape for {@code POST /auth/login} and {@code POST /auth/renew} (LR-B6).
 * {@code accessTokenExpiresAt} / {@code sessionAbsoluteDeadline} are ISO-8601 UTC instants the
 * frontend renewal scheduler relies on (it never parses the token or trusts the local clock).
 */
public record LoginResult(
        String accessToken,
        String tokenType,
        Instant accessTokenExpiresAt,
        Instant sessionAbsoluteDeadline,
        ManagementSessionView session
) {
}
