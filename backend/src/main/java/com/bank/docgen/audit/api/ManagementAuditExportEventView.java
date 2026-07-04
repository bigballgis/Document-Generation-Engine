package com.bank.docgen.audit.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.time.Instant;
import java.util.List;

public record ManagementAuditExportEventView(
        Instant eventAt,
        String eventType,
        String templateId,
        String credentialId,
        Integer previousPolicyVersion,
        Integer policyVersion,
        List<String> changedAreas,
        boolean rollback,
        Integer rollbackSourcePolicyVersion,
        String actorSummaryMasked,
        String credentialFingerprintMasked,
        String statusSummary,
        List<String> warningCodes
) {
    public ManagementAuditExportEventView {
        changedAreas = DefensiveCopies.copyList(changedAreas);
        warningCodes = DefensiveCopies.copyStringList(warningCodes);
    }

}
