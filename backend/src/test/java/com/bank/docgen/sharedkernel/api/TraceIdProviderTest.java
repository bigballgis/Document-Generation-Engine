package com.bank.docgen.sharedkernel.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;

class TraceIdProviderTest {

    @Test
    void activeSpanTraceIdPrecedesHeader() {
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext context = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(context);
        when(context.traceId()).thenReturn("otel-trace-123");

        TraceIdProvider provider = new TraceIdProvider(tracer);

        assertThat(provider.currentOrNew("header-trace-456")).isEqualTo("otel-trace-123");
    }

    @Test
    void headerUsedWhenNoActiveSpan() {
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(null);

        TraceIdProvider provider = new TraceIdProvider(tracer);

        assertThat(provider.currentOrNew("header-trace-456")).isEqualTo("header-trace-456");
    }

    @Test
    void headerTrimmedWhenNoActiveSpan() {
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(null);

        TraceIdProvider provider = new TraceIdProvider(tracer);

        assertThat(provider.currentOrNew("  header-trace-456  ")).isEqualTo("header-trace-456");
    }

    @Test
    void uuidFallbackWhenNoSpanOrHeader() {
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(null);

        TraceIdProvider provider = new TraceIdProvider(tracer);

        assertThat(provider.currentOrNew(null)).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        );
    }

    @Test
    void blankHeaderIgnoredWhenNoActiveSpan() {
        Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(null);

        TraceIdProvider provider = new TraceIdProvider(tracer);

        assertThat(provider.currentOrNew("   ")).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
        );
    }

    @Test
    void noArgConstructorFallsBackToHeaderThenUuid() {
        TraceIdProvider provider = new TraceIdProvider();

        assertThat(provider.currentOrNew("legacy-header")).isEqualTo("legacy-header");
    }

    @Test
    void newAuditIdHasPrefix() {
        TraceIdProvider provider = new TraceIdProvider();

        assertThat(provider.newAuditId()).startsWith("AUD-");
    }
}
