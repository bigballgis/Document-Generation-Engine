package com.bank.docgen.audit.api;

import java.util.List;

public record ManagementAuditQueryResult(List<ManagementAuditEventView> events) {
}
