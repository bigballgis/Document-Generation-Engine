package com.bank.docgen.audit.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record LifecycleAuditExportResult(String format, List<LifecycleAuditEventView> events) {
    public LifecycleAuditExportResult {
        events = DefensiveCopies.copyList(events);
    }
}
