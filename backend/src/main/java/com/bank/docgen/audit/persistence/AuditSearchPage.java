package com.bank.docgen.audit.persistence;

import java.util.List;

public record AuditSearchPage<T>(List<T> content, long totalElements, int totalPages) {
}
