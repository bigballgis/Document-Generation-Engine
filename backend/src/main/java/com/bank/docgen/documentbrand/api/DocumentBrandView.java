package com.bank.docgen.documentbrand.api;

import com.bank.docgen.documentbrand.domain.DocumentBrandStatus;

public record DocumentBrandView(
        String groupCode,
        String documentBrandCode,
        String displayName,
        DocumentBrandStatus status,
        String logoObjectRef,
        String defaultSealObjectRef,
        String letterheadLegalName
) {
}
