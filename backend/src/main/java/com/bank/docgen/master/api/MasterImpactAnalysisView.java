package com.bank.docgen.master.api;

import java.util.List;

public record MasterImpactAnalysisView(
        String masterId,
        List<String> referencedTemplateIds,
        boolean retestRequired
) {
}
