package com.bank.docgen.rendering.api;

/**
 * Optional composition / document-brand axes for management preview / test-generate
 * (ADR-0063 / E2-C9; ADR-0065 / E4).
 * Mirrors runtime {@code context.jurisdiction} / {@code product} / {@code channel} /
 * {@code legalEntityCode}.
 */
public record PreviewCompositionContext(
        String jurisdiction,
        String product,
        String channel,
        String legalEntityCode
) {
    /** Compatibility constructor for callers that omit legalEntityCode. */
    public PreviewCompositionContext(String jurisdiction, String product, String channel) {
        this(jurisdiction, product, channel, null);
    }
}
