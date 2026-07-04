package com.bank.docgen.authorization.management.api;

import java.time.Instant;
import java.util.List;

public record ManagementUserView(
        String id,
        String username,
        String displayName,
        String email,
        String authSource,
        List<String> roles,
        List<String> authorizedGroupCodes,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
    public ManagementUserView {
        roles = copyStrings(roles);
        authorizedGroupCodes = copyStrings(authorizedGroupCodes);
    }

    private static List<String> copyStrings(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
