package com.bank.docgen.sharedkernel.security;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.sharedkernel.api.DefensiveCopies;
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
    public ManagementSessionClaims {
        roles = DefensiveCopies.copyStringList(roles);
        authorizedGroupCodes = DefensiveCopies.copyStringList(authorizedGroupCodes);
        visibleRoutes = DefensiveCopies.copyStringList(visibleRoutes);
    }
}
