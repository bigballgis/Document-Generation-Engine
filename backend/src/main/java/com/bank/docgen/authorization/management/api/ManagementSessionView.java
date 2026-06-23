package com.bank.docgen.authorization.management.api;

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
}
