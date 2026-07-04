package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record PublishGateChecklistView(
        String templateId,
        boolean ready,
        int blockerCount,
        List<PublishGateItemView> items
) {
    public PublishGateChecklistView {
        items = DefensiveCopies.copyList(items);
    }
}
