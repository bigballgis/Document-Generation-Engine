package com.bank.docgen.authorization.management.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.util.List;

/**
 * Entity-level page result from catalog query repositories (before view mapping).
 */
public record CatalogQueryPage<T>(List<T> content, long totalElements, int totalPages) {

    public CatalogQueryPage {
        content = DefensiveCopies.copyList(content);
    }
}
