package com.bank.docgen.runtime.api;

/**
 * Optional non-sensitive tracking / composition context for runtime generate requests
 * (OpenAPI {@code Context}).
 * Whitelist-only: unknown subfields are rejected by runtime strict deserialization.
 * ADR-0063 adds optional {@code jurisdiction} / {@code product}; {@code channel} also
 * participates in Composition Inclusion Rule matching.
 */
public record ContextView(
        String sourceSystem,
        String channel,
        String businessRequestId,
        String upstreamTraceId,
        String scenario,
        String locale,
        String jurisdiction,
        String product
) {
    /** Compatibility constructor for callers that omit IBL-E2 composition axes. */
    public ContextView(
            String sourceSystem,
            String channel,
            String businessRequestId,
            String upstreamTraceId,
            String scenario,
            String locale
    ) {
        this(sourceSystem, channel, businessRequestId, upstreamTraceId, scenario, locale, null, null);
    }
}
