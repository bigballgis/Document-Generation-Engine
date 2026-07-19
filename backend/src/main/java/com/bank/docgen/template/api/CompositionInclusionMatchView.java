package com.bank.docgen.template.api;

/**
 * ADR-0063 match axes. At least one of jurisdiction / product / channel must be non-blank after trim.
 */
public record CompositionInclusionMatchView(
        String jurisdiction,
        String product,
        String channel
) {
}
