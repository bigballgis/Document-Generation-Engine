package com.bank.docgen.audit.api;

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
}
