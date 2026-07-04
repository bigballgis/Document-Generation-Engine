package com.bank.docgen.authorization.management.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
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
        roles = DefensiveCopies.copyStringList(roles);
        authorizedGroupCodes = DefensiveCopies.copyStringList(authorizedGroupCodes);
    }
}
