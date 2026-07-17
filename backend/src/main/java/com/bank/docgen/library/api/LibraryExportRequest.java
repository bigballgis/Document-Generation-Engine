package com.bank.docgen.library.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;
import java.util.UUID;

/**
 * Optional filters for CE-E03 full-library export.
 */
public record LibraryExportRequest(
        UUID groupId,
        List<UUID> templateIds,
        Boolean includeSkipped
) {
    public LibraryExportRequest {
        templateIds = DefensiveCopies.copyList(templateIds);
    }

    public boolean includeSkippedOrDefault() {
        return includeSkipped == null || includeSkipped;
    }
}
