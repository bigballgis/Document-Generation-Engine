package com.bank.docgen.sharedkernel.api;

/**
 * Shared correlation identifiers for HTTP, MDC, and Kafka transport headers (ADR-0047 / ADR-0049).
 */
public final class TraceIdConstants {

    public static final String MDC_KEY = "traceId";

    public static final String HEADER_NAME = "X-Trace-Id";

    private TraceIdConstants() {
    }
}
