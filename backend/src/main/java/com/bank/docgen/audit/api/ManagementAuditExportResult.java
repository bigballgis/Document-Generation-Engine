package com.bank.docgen.audit.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record ManagementAuditExportResult(String format, List<ManagementAuditExportEventView> events) {
    public ManagementAuditExportResult {
        events = DefensiveCopies.copyList(events);
    }
}
