package com.bank.docgen.sharedkernel.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

/**
 * BDD-CE-O01-001 / 008 — pdfArchivalProfile default NONE; unknown enum fail-closed.
 */
class RenderProfilePdfArchivalTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void missingFieldDefaultsToNone() throws Exception {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("renderProfileVersion", "rp-v1");

        RenderProfile profile = RenderProfile.fromJsonNode(node);

        assertThat(profile.pdfArchivalProfile()).isEqualTo(PdfArchivalProfile.NONE);
    }

    @Test
    void explicitNoneIsPreserved() throws Exception {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("pdfArchivalProfile", "NONE");

        RenderProfile profile = RenderProfile.fromJsonNode(node);

        assertThat(profile.pdfArchivalProfile()).isEqualTo(PdfArchivalProfile.NONE);
    }

    @Test
    void pdfA2bIsAccepted() throws Exception {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("pdfArchivalProfile", "PDF_A_2B");

        RenderProfile profile = RenderProfile.fromJsonNode(node);

        assertThat(profile.pdfArchivalProfile()).isEqualTo(PdfArchivalProfile.PDF_A_2B);
    }

    @Test
    void unknownEnumFailsClosed() throws Exception {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("pdfArchivalProfile", "PDF_A_1B");

        assertThatThrownBy(() -> RenderProfile.fromJsonNode(node))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("api.error.rendering.renderProfileInvalid");
    }
}
