package com.bank.docgen.authorization.management.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.time.Instant;
import java.util.List;

public record ManagementSessionView(
        String username,
        String displayName,
        String email,
        String authSource,
        List<String> roles,
        List<String> authorizedGroupCodes,
        String defaultRoute,
        List<String> visibleRoutes,
        ManagementCapabilitiesView capabilities,
        Instant expiresAt
) {
    public ManagementSessionView {
        roles = DefensiveCopies.copyStringList(roles);
        authorizedGroupCodes = DefensiveCopies.copyStringList(authorizedGroupCodes);
        visibleRoutes = DefensiveCopies.copyStringList(visibleRoutes);
    }
}
