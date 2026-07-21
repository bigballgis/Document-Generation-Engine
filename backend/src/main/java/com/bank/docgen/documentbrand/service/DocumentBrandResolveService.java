package com.bank.docgen.documentbrand.service;

import com.bank.docgen.documentbrand.domain.ResolvedDocumentBrand;
import java.util.Collection;
import org.springframework.stereotype.Service;

/**
 * ADR-0071 / SYS-NORM Wave 6 — runtime simplify: no LegalEntity→DocumentBrand catalog
 * resolve. {@code legalEntityCode} and {@code allowedDocumentBrandCodes} are ignored for
 * gating; letterhead/logo/seal come from Letterhead (master) bindings.
 */
@Service
public class DocumentBrandResolveService {

    /**
     * Returns a letterhead-neutral resolution. Catalog lookups and allow-list gates are
     * retired (BDD-SYS-NORM-D1-012…015).
     */
    public ResolvedDocumentBrand resolve(
            String groupCode,
            String legalEntityCodeRaw,
            Collection<String> allowedDocumentBrandCodes
    ) {
        return ResolvedDocumentBrand.letterheadNeutral();
    }
}
