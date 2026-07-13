package com.bank.docgen.apimgmt.web;

import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Shared SuccessEnvelope wrapping for API management web endpoints.
 */
final class ApiManagementWebEnvelopeSupport {

    private final TraceIdProvider traceIdProvider;

    ApiManagementWebEnvelopeSupport(TraceIdProvider traceIdProvider) {
        this.traceIdProvider = traceIdProvider;
    }

    <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
