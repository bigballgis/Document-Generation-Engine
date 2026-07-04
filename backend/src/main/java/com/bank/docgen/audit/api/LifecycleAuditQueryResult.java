package com.bank.docgen.audit.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record LifecycleAuditQueryResult(
        List<LifecycleAuditEventView> events,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public LifecycleAuditQueryResult {
        events = DefensiveCopies.copyList(events);
    }

    public LifecycleAuditQueryResult(List<LifecycleAuditEventView> events) {
        this(events, 0, AuditPagedResult.DEFAULT_PAGE_SIZE, events.size(), 1);
    }
}
