package com.bank.docgen.master.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;

public record MasterImpactAnalysisView(
        String masterId,
        List<String> referencedTemplateIds,
        boolean retestRequired
) {
    public MasterImpactAnalysisView {
        referencedTemplateIds = DefensiveCopies.copyList(referencedTemplateIds);
    }

}
