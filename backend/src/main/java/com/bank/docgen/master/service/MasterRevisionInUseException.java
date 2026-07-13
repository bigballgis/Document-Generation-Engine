package com.bank.docgen.master.service;

import com.bank.docgen.master.api.PinnedReleaseReference;
import java.util.List;

/**
 * Raised when a master revision line is referenced by one or more non-deleted
 * published-lifecycle template versions and therefore cannot be physically or
 * logically deleted (CE-K01 fail-closed).
 */
public class MasterRevisionInUseException extends RuntimeException {

    private final List<PinnedReleaseReference> references;

    public MasterRevisionInUseException(List<PinnedReleaseReference> references) {
        super("api.error.master.revisionInUseByPublishedRelease");
        this.references = List.copyOf(references);
    }

    public List<PinnedReleaseReference> references() {
        return references;
    }
}
