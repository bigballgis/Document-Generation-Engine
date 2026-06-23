package com.bank.docgen.audit.api;

import java.util.List;

public record ManagementAuditExportResult(String format, List<ManagementAuditExportEventView> events) {
}
