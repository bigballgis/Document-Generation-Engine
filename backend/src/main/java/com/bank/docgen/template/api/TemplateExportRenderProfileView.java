package com.bank.docgen.template.api;

/**
 * CE-E01 render profile snapshot for v2 export bundles.
 */
public record TemplateExportRenderProfileView(
        String version,
        String json
) {
}
