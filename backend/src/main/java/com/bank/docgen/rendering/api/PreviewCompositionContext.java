package com.bank.docgen.rendering.api;

/**
 * Optional composition axes for management preview / test-generate (ADR-0063 / E2-C9).
 * Mirrors runtime {@code context.jurisdiction} / {@code product} / {@code channel}.
 */
public record PreviewCompositionContext(
        String jurisdiction,
        String product,
        String channel
) {
}
