package com.bank.docgen.documentbrand.api;

import com.bank.docgen.documentbrand.domain.DocumentBrandStatus;

public record LegalEntityView(
        String groupCode,
        String legalEntityCode,
        String displayName,
        DocumentBrandStatus status,
        String documentBrandCode
) {
}
