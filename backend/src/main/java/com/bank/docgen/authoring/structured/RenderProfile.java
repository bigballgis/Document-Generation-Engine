package com.bank.docgen.authoring.structured;

import com.fasterxml.jackson.databind.JsonNode;

public record RenderProfile(
        String renderProfileVersion,
        String styleMappingPolicy,
        String numberingBehavior,
        String tablePaginationPolicy,
        String imageScalingPolicy,
        String pdfConversionPolicy,
        String fidelityPolicy
) {

    public static RenderProfile fromJsonNode(JsonNode node) {
        return new RenderProfile(
                node.path("renderProfileVersion").asText("rp-v1"),
                node.path("styleMappingPolicy").asText("MASTER_CATALOG_LOCKED"),
                node.path("numberingBehavior").asText("CONTROLLED_MULTILEVEL"),
                node.path("tablePaginationPolicy").asText("REPEAT_HEADER"),
                node.path("imageScalingPolicy").asText("PROPORTIONAL_FIT"),
                node.path("pdfConversionPolicy").asText("SEMANTIC_FIDELITY"),
                node.path("fidelityPolicy").asText("BLOCKERS_PREVENT_PUBLISH")
        );
    }
}
