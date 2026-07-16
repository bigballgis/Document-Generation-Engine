package com.bank.docgen.sharedkernel.document;

import com.fasterxml.jackson.databind.JsonNode;

public record RenderProfile(
        String renderProfileVersion,
        String styleMappingPolicy,
        String numberingBehavior,
        String tablePaginationPolicy,
        String imageScalingPolicy,
        String pdfConversionPolicy,
        String fidelityPolicy,
        boolean pdfPageNumberStampingEnabled,
        PdfArchivalProfile pdfArchivalProfile
) {

    public RenderProfile {
        if (pdfArchivalProfile == null) {
            pdfArchivalProfile = PdfArchivalProfile.NONE;
        }
    }

    public static RenderProfile fromJsonNode(JsonNode node) {
        return new RenderProfile(
                node.path("renderProfileVersion").asText("rp-v1"),
                node.path("styleMappingPolicy").asText("MASTER_CATALOG_LOCKED"),
                node.path("numberingBehavior").asText("CONTROLLED_MULTILEVEL"),
                node.path("tablePaginationPolicy").asText("REPEAT_HEADER"),
                node.path("imageScalingPolicy").asText("PROPORTIONAL_FIT"),
                node.path("pdfConversionPolicy").asText("SEMANTIC_FIDELITY"),
                node.path("fidelityPolicy").asText("BLOCKERS_PREVENT_PUBLISH"),
                node.path("pdfPageNumberStampingEnabled").asBoolean(false),
                parsePdfArchivalProfile(node)
        );
    }

    /**
     * Missing / null / blank → {@link PdfArchivalProfile#NONE}. Explicit unknown values fail closed.
     */
    static PdfArchivalProfile parsePdfArchivalProfile(JsonNode node) {
        JsonNode field = node.get("pdfArchivalProfile");
        if (field == null || field.isNull()) {
            return PdfArchivalProfile.NONE;
        }
        String raw = field.asText(null);
        if (raw == null || raw.isBlank()) {
            return PdfArchivalProfile.NONE;
        }
        try {
            return PdfArchivalProfile.valueOf(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("api.error.rendering.renderProfileInvalid");
        }
    }
}
