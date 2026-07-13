package com.bank.docgen.template.api;

import com.bank.docgen.authoring.structured.PasteCleaningCategory;
import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

/**
 * Non-sensitive paste-cleaning residue persisted on an anchor binding (ADR-0019 /
 * ops-paste-binding-seam). Must not carry source HTML or pasted plaintext.
 */
public record PasteCleaningEvidenceView(
        int transformedCount,
        int removedCount,
        int warningCount,
        int blockedCount,
        Boolean unresolvedPasteBlockers,
        List<PasteCleaningEvidenceItemView> items
) {

    public PasteCleaningEvidenceView {
        items = DefensiveCopies.copyList(items);
    }

    public boolean hasUnresolvedPasteBlockers() {
        if (Boolean.TRUE.equals(unresolvedPasteBlockers) || blockedCount > 0) {
            return true;
        }
        if (items == null) {
            return false;
        }
        return items.stream().anyMatch(item -> item.category() == PasteCleaningCategory.BLOCKED);
    }
}
