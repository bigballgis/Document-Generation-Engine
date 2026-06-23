package com.bank.docgen.audit.api;

import java.util.List;

public record LifecycleAuditExportResult(String format, List<LifecycleAuditEventView> events) {
}
