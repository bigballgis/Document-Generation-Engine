package com.bank.docgen.audit.api;

import java.util.List;

public record PolicyUpdateAuditDetail(
        List<String> configDiffSummary,
        List<String> impactPreviewSummary,
        List<String> hardBlockSummary,
        List<String> warningSummary,
        boolean confirmed,
        Boolean rollback,
        Integer rollbackSourcePolicyVersion
) {

    public static PolicyUpdateAuditDetail empty() {
        return new PolicyUpdateAuditDetail(List.of(), List.of(), List.of(), List.of(), false, false, null);
    }
}
