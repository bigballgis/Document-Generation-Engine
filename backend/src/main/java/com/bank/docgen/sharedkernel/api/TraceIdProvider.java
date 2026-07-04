package com.bank.docgen.sharedkernel.api;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class TraceIdProvider {

    private final Tracer tracer;

    public TraceIdProvider(ObjectProvider<Tracer> tracerProvider) {
        this(tracerProvider.getIfAvailable(() -> null));
    }

    TraceIdProvider(Tracer tracer) {
        this.tracer = tracer;
    }

    public TraceIdProvider() {
        this((Tracer) null);
    }

    public String currentOrNew(String incomingTraceId) {
        String activeTraceId = activeSpanTraceId();
        if (activeTraceId != null) {
            return activeTraceId;
        }
        if (incomingTraceId != null && !incomingTraceId.isBlank()) {
            return incomingTraceId.trim();
        }
        return UUID.randomUUID().toString();
    }

    public String newAuditId() {
        return "AUD-" + UUID.randomUUID();
    }

    private String activeSpanTraceId() {
        if (tracer == null) {
            return null;
        }
        Span currentSpan = tracer.currentSpan();
        if (currentSpan == null) {
            return null;
        }
        String traceId = currentSpan.context().traceId();
        if (traceId == null || traceId.isBlank()) {
            return null;
        }
        return traceId;
    }
}
