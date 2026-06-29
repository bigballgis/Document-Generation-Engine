package com.bank.docgen.template.api;

import java.util.Set;

public record MasterStyleCatalogEntryView(
        String styleKey,
        Set<String> applicableNodeTypes,
        String renderPurpose
) {
}
