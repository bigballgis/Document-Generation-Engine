package com.bank.docgen.audit.api;

import java.time.Instant;
import java.util.List;

public record LifecycleAuditEventView(
        Instant eventAt,
        String eventType,
        String templateId,
        String operation,
        String fromState,
        String toState,
        String actorId,
        String summary,
        List<String> warningCodes
) {
}
