package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import com.bank.docgen.template.domain.ChangeDiffDimension;
import java.util.List;

public record ChangeDiffDimensionView(
        ChangeDiffDimension dimension,
        List<String> added,
        List<String> removed,
        List<ChangeDiffModificationView> modified
) {
    public ChangeDiffDimensionView {
        added = DefensiveCopies.copyList(added);
        removed = DefensiveCopies.copyList(removed);
        modified = DefensiveCopies.copyList(modified);
    }

}
