package com.bank.docgen.apimgmt.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.util.List;

public record ApiPolicyImpactPreviewView(
        List<String> changedAreas,
        boolean blocking,
        List<String> warnings,
        boolean defaultRouteImpacted,
        int currentPolicyVersion,
        int nextPolicyVersion,
        String summaryMessageKey,
        String contractDiffSummary,
        String idempotencyImpactSummary
) {
    public ApiPolicyImpactPreviewView {
        changedAreas = DefensiveCopies.copyList(changedAreas);
        warnings = DefensiveCopies.copyList(warnings);
    }

}
