package com.bank.docgen.template.api;

import java.util.List;

public record PublishGateChecklistView(
        String templateId,
        boolean ready,
        int blockerCount,
        List<PublishGateItemView> items
) {
}
