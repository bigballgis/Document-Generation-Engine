package com.bank.docgen.audit.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.time.Instant;
import java.util.List;

public record LifecycleAuditEventView(
        Instant eventAt,
        String eventType,
        String templateId,
        String templateDisplayName,
        String templateExternalId,
        String operation,
        String fromState,
        String toState,
        String actorId,
        String actorDisplayName,
        String summary,
        List<String> warningCodes
) {
    public LifecycleAuditEventView {
        warningCodes = DefensiveCopies.copyStringList(warningCodes);
    }
}
