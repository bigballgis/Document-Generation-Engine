package com.bank.docgen.master.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;

public record MasterImpactAnalysisView(
        String masterId,
        List<String> referencedTemplateIds,
        List<MasterReferencedTemplateView> referencedTemplates,
        boolean retestRequired,
        MasterAnchorSetDeltaView anchorDelta
) {
    public MasterImpactAnalysisView {
        referencedTemplateIds = DefensiveCopies.copyList(referencedTemplateIds);
        referencedTemplates = DefensiveCopies.copyList(referencedTemplates);
    }

    /**
     * Backward-compatible constructor used by transitional stubs/tests.
     */
    public MasterImpactAnalysisView(
            String masterId,
            List<String> referencedTemplateIds,
            boolean retestRequired
    ) {
        this(
                masterId,
                referencedTemplateIds,
                List.of(),
                retestRequired,
                new MasterAnchorSetDeltaView(List.of(), List.of(), List.of())
        );
    }
}
