package com.bank.docgen.audit.persistence;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

public record AuditSearchPage<T>(List<T> content, long totalElements, int totalPages) {

    public AuditSearchPage {
        content = DefensiveCopies.copyList(content);
    }
}
