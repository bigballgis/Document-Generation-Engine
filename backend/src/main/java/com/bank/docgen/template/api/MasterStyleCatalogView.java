package com.bank.docgen.template.api;

import java.util.List;

public record MasterStyleCatalogView(
        String catalogVersion,
        List<MasterStyleCatalogEntryView> entries
) {
}
