package com.bank.docgen.audit.api;

import java.util.List;

public record LifecycleAuditQueryResult(List<LifecycleAuditEventView> events) {
}
