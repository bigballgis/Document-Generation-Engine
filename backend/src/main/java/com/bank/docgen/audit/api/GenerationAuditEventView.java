package com.bank.docgen.audit.api;

import java.time.Instant;

public record GenerationAuditEventView(
        Instant eventAt,
        String eventType,
        String templateExternalId,
        String requestId,
        String outcome,
        String status,
        String accessAccountSummary
) {
}
