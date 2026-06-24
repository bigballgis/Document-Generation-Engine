package com.bank.docgen.audit.api;

import java.util.List;

public record ManagementAuditQueryResult(
        List<ManagementAuditEventView> events,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public ManagementAuditQueryResult(List<ManagementAuditEventView> events) {
        this(events, 0, AuditPagedResult.DEFAULT_PAGE_SIZE, events.size(), 1);
    }
}
