package com.bank.docgen.documentbrand.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.documentbrand.domain.ResolvedDocumentBrand;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * BDD-SYS-NORM-D1-012…015 — ADR-0071 runtime simplify: no LegalEntity→DocumentBrand catalog
 * resolve; allow-list non-gating; legalEntityCode opaque non-driving.
 */
class DocumentBrandResolveServiceTest {

    private DocumentBrandResolveService service;

    @BeforeEach
    void setUp() {
        service = new DocumentBrandResolveService();
    }

    @Test
    void omitLegalEntity_succeedsWithoutCatalog_bddD1012() {
        ResolvedDocumentBrand resolved = service.resolve("RETAIL", null, List.of());

        assertThat(resolved.legalEntityCode()).isNull();
        assertThat(resolved.documentBrandCode()).isNull();
        assertThat(resolved.logoObjectRef()).isNull();
        assertThat(resolved.defaultSealObjectRef()).isNull();
        assertThat(resolved.letterheadLegalName()).isNull();
    }

    @Test
    void presentLegalEntity_isNonDriving_bddD1013() {
        ResolvedDocumentBrand resolved = service.resolve("RETAIL", "NO-SUCH-ENTITY", List.of());

        assertThat(resolved.legalEntityCode()).isNull();
        assertThat(resolved.documentBrandCode()).isNull();
        assertThat(resolved.logoObjectRef()).isNull();
    }

    @Test
    void allowListDoesNotGate_bddD1014() {
        ResolvedDocumentBrand resolved = service.resolve(
                "RETAIL",
                "LE-HK-001",
                List.of("HK-RETAIL-LETTER", "UK-CORP-LETTER")
        );

        assertThat(resolved.documentBrandCode()).isNull();
        assertThat(resolved.logoObjectRef()).isNull();
    }

    @Test
    void blankGroupStillReturnsLetterheadNeutral_bddD1012() {
        ResolvedDocumentBrand resolved = service.resolve("  ", "LE-X", List.of("BRAND"));

        assertThat(resolved).isEqualTo(ResolvedDocumentBrand.letterheadNeutral());
    }
}
