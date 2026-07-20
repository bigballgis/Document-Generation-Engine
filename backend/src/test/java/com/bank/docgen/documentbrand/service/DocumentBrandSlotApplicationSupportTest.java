package com.bank.docgen.documentbrand.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.documentbrand.domain.ResolvedDocumentBrand;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * BDD-IBL-E4-008 / 009 — apply brand assets; explicit sealRef wins.
 */
class DocumentBrandSlotApplicationSupportTest {

    @Test
    void appliesLogoAndLetterhead_bddE4008() {
        ResolvedDocumentBrand brand = new ResolvedDocumentBrand(
                "LE-HK-001",
                "HK-RETAIL-LETTER",
                "logo-hk",
                "seal-hk",
                "HK Retail Bank Ltd"
        );
        Map<String, String> bindings = new LinkedHashMap<>();
        bindings.put("A1", """
                {"type":"imageRef","documentBrandSlot":"LOGO","imageRef":"old-logo"}
                """);
        bindings.put("A2", """
                {"type":"paragraph","children":[{"type":"text","documentBrandSlot":"LETTERHEAD_LEGAL_NAME","text":""}]}
                """);

        DocumentBrandSlotApplicationSupport.Applied applied =
                DocumentBrandSlotApplicationSupport.apply(bindings, brand);

        assertThat(applied.bindingJson().get("A1")).contains("\"imageRef\":\"logo-hk\"");
        assertThat(applied.bindingJson().get("A2")).contains("HK Retail Bank Ltd");
        assertThat(applied.fidelityWarningCodes()).isEmpty();
        assertThat(applied.slotsApplied()).isTrue();
    }

    @Test
    void explicitSealWinsOverBrandDefault_bddE4009() {
        ResolvedDocumentBrand brand = new ResolvedDocumentBrand(
                "LE-HK-001",
                "HK-RETAIL-LETTER",
                "logo-hk",
                "seal-brand",
                null
        );
        Map<String, String> bindings = Map.of(
                "SEAL",
                """
                {"type":"sealRef","documentBrandSlot":"DEFAULT_SEAL","referenceKey":"seal-explicit"}
                """
        );

        DocumentBrandSlotApplicationSupport.Applied applied =
                DocumentBrandSlotApplicationSupport.apply(bindings, brand);

        assertThat(applied.bindingJson().get("SEAL")).contains("seal-explicit");
        assertThat(applied.bindingJson().get("SEAL")).doesNotContain("seal-brand");
    }

    @Test
    void emptySealSlotGetsBrandDefault() {
        ResolvedDocumentBrand brand = new ResolvedDocumentBrand(
                "LE-HK-001",
                "HK-RETAIL-LETTER",
                "logo-hk",
                "seal-brand",
                null
        );
        Map<String, String> bindings = Map.of(
                "SEAL",
                """
                {"type":"sealRef","documentBrandSlot":"DEFAULT_SEAL","referenceKey":""}
                """
        );

        DocumentBrandSlotApplicationSupport.Applied applied =
                DocumentBrandSlotApplicationSupport.apply(bindings, brand);

        assertThat(applied.bindingJson().get("SEAL")).contains("seal-brand");
    }

    @Test
    void missingSlots_emitsNonBlockingWarning_forEntityBrand() {
        ResolvedDocumentBrand brand = new ResolvedDocumentBrand(
                "LE-HK-001",
                "HK-RETAIL-LETTER",
                "logo",
                null,
                null
        );

        DocumentBrandSlotApplicationSupport.Applied applied =
                DocumentBrandSlotApplicationSupport.apply(Map.of("A1", "{\"type\":\"paragraph\"}"), brand);

        assertThat(applied.fidelityWarningCodes())
                .containsExactly(DocumentBrandSlotApplicationSupport.WARNING_SLOTS_ABSENT);
        assertThat(applied.slotsApplied()).isFalse();
    }

    @Test
    void missingSlots_platformDefault_doesNotEmitWarning() {
        ResolvedDocumentBrand brand = new ResolvedDocumentBrand(
                null,
                DocumentBrandCodes.PLATFORM_DEFAULT,
                DocumentBrandCodes.PLATFORM_DEFAULT_LOGO_REF,
                null,
                null
        );

        DocumentBrandSlotApplicationSupport.Applied applied =
                DocumentBrandSlotApplicationSupport.apply(Map.of("A1", "{\"type\":\"paragraph\"}"), brand);

        assertThat(applied.fidelityWarningCodes()).isEmpty();
        assertThat(applied.slotsApplied()).isFalse();
    }
}
