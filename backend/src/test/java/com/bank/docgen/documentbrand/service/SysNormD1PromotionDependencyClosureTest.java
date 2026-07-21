package com.bank.docgen.documentbrand.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.template.domain.TemplateImportDependencyType;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * BDD-SYS-NORM-D1-005 / D1-C11 — promotion/export dependency closure must not require
 * DocumentBrand / LegalEntity sidecar catalogs (Wave 7 owns dry-run UI).
 */
class SysNormD1PromotionDependencyClosureTest {

    @Test
    void importDependencyTypesOmitBrandAndEntitySidecars_bddD1005() {
        Set<String> types = Arrays.stream(TemplateImportDependencyType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(types).doesNotContain(
                "DOCUMENT_BRAND",
                "LEGAL_ENTITY",
                "DOCUMENT_BRAND_CATALOG",
                "LEGAL_ENTITY_CATALOG",
                "DEFAULT_LEGAL_ENTITY"
        );
        assertThat(types).contains("MASTER_PIN", "BUNDLE_FORMAT");
    }
}
