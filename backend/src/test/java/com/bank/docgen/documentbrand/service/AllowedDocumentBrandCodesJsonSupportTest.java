package com.bank.docgen.documentbrand.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * BDD-SYS-NORM-D1-014 / D1-C7 — Wave 6 locks strip-to-empty on write (OpenAPI-aligned).
 */
class AllowedDocumentBrandCodesJsonSupportTest {

    @Test
    void write_stripsNonEmptyAllowListToNull() {
        assertThat(AllowedDocumentBrandCodesJsonSupport.write(List.of("HK-RETAIL-LETTER"))).isNull();
        assertThat(AllowedDocumentBrandCodesJsonSupport.write(List.of("A", "B"))).isNull();
    }

    @Test
    void write_emptyOrNullRemainsNull() {
        assertThat(AllowedDocumentBrandCodesJsonSupport.write(List.of())).isNull();
        assertThat(AllowedDocumentBrandCodesJsonSupport.write(null)).isNull();
    }

    @Test
    void parse_historicalJsonStillReadableAsEmptyOrValues() {
        assertThat(AllowedDocumentBrandCodesJsonSupport.parse(null)).isEmpty();
        assertThat(AllowedDocumentBrandCodesJsonSupport.parse("[\"HK-RETAIL-LETTER\"]"))
                .containsExactly("HK-RETAIL-LETTER");
    }
}
