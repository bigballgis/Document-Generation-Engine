package com.bank.docgen.library.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;
import java.util.UUID;

/**
 * Optional filters for CE-E03 full-library export.
 * Wave 7: optional {@code dependencyClosure=PROMOTION}.
 */
public record LibraryExportRequest(
        UUID groupId,
        List<UUID> templateIds,
        Boolean includeSkipped,
        String dependencyClosure
) {
    public LibraryExportRequest {
        templateIds = DefensiveCopies.copyList(templateIds);
    }

    public LibraryExportRequest(UUID groupId, List<UUID> templateIds, Boolean includeSkipped) {
        this(groupId, templateIds, includeSkipped, null);
    }

    public boolean includeSkippedOrDefault() {
        return includeSkipped == null || includeSkipped;
    }
}
