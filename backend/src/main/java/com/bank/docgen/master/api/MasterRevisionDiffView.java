package com.bank.docgen.master.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

/**
 * Revision A vs B comparison: anchor-set delta + SHA-256 file hashes (CE-K05).
 */
public record MasterRevisionDiffView(
        String masterId,
        String baselineRevisionLineId,
        String candidateRevisionLineId,
        List<String> addedAnchors,
        List<String> removedAnchors,
        List<MasterRenamedAnchorView> renamedAnchors,
        String baselineFileHash,
        String candidateFileHash
) {
    public MasterRevisionDiffView {
        addedAnchors = DefensiveCopies.copyList(addedAnchors);
        removedAnchors = DefensiveCopies.copyList(removedAnchors);
        renamedAnchors = DefensiveCopies.copyList(renamedAnchors);
    }
}
