package com.bank.docgen.apimgmt.api;

import java.util.List;

public record ApiPolicyImpactPreviewView(
        List<String> changedAreas,
        boolean blocking,
        List<String> warnings,
        boolean defaultRouteImpacted,
        int currentPolicyVersion,
        int nextPolicyVersion,
        String summaryMessageKey
) {
}
