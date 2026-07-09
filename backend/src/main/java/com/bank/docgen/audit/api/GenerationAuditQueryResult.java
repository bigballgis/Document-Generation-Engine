package com.bank.docgen.audit.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record GenerationAuditQueryResult(
        List<GenerationAuditEventView> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public GenerationAuditQueryResult {
        content = DefensiveCopies.copyList(content);
    }
}
