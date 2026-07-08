package com.bank.docgen.audit.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;

import java.time.Instant;
import java.util.List;

public record ManagementAuditEventView(
        Instant eventAt,
        String eventType,
        String templateId,
        String templateDisplayName,
        String templateExternalId,
        String credentialId,
        Integer previousPolicyVersion,
        Integer policyVersion,
        List<String> changedAreas,
        boolean rollback,
        Integer rollbackSourcePolicyVersion,
        String actorSummary,
        String credentialFingerprint,
        String statusSummary,
        List<String> warningCodes,
        String requestId
) {
    public ManagementAuditEventView {
        changedAreas = DefensiveCopies.copyList(changedAreas);
        warningCodes = DefensiveCopies.copyStringList(warningCodes);
    }

}
