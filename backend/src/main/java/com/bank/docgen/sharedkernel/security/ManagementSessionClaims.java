package com.bank.docgen.sharedkernel.security;

import com.bank.docgen.authorization.management.domain.AuthSource;
import java.time.Instant;
import java.util.List;

public record ManagementSessionClaims(
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
}
