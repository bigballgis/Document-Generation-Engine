package com.bank.docgen.runtime.api;

/**
 * Optional non-sensitive tracking context for runtime generate requests (OpenAPI {@code Context}).
 * Whitelist-only: six string fields; unknown subfields are rejected by runtime strict deserialization.
 */
public record ContextView(
        String sourceSystem,
        String channel,
        String businessRequestId,
        String upstreamTraceId,
        String scenario,
        String locale
) {
}
