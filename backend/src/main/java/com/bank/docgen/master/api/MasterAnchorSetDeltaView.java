package com.bank.docgen.master.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

/**
 * Anchor-set delta between baseline and candidate master revisions (CE-K05).
 */
public record MasterAnchorSetDeltaView(
        List<String> addedAnchors,
        List<String> removedAnchors,
        List<MasterRenamedAnchorView> renamedAnchors
) {
    public MasterAnchorSetDeltaView {
        addedAnchors = DefensiveCopies.copyList(addedAnchors);
        removedAnchors = DefensiveCopies.copyList(removedAnchors);
        renamedAnchors = DefensiveCopies.copyList(renamedAnchors);
    }

    public boolean isEmpty() {
        return addedAnchors.isEmpty() && removedAnchors.isEmpty() && renamedAnchors.isEmpty();
    }
}
