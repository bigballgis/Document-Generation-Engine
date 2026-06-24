package com.bank.docgen.audit.api;

import java.util.List;

public record LifecycleAuditQueryResult(
        List<LifecycleAuditEventView> events,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public LifecycleAuditQueryResult(List<LifecycleAuditEventView> events) {
        this(events, 0, AuditPagedResult.DEFAULT_PAGE_SIZE, events.size(), 1);
    }
}
