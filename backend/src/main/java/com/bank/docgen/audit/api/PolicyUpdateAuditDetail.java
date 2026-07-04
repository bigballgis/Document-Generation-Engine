package com.bank.docgen.audit.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

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
    public PolicyUpdateAuditDetail {
        configDiffSummary = DefensiveCopies.copyList(configDiffSummary);
        impactPreviewSummary = DefensiveCopies.copyList(impactPreviewSummary);
        hardBlockSummary = DefensiveCopies.copyList(hardBlockSummary);
        warningSummary = DefensiveCopies.copyList(warningSummary);
    }


    public static PolicyUpdateAuditDetail empty() {
        return new PolicyUpdateAuditDetail(List.of(), List.of(), List.of(), List.of(), false, false, null);
    }
}
