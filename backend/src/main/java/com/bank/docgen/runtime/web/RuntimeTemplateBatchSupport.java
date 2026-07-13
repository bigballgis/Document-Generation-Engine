package com.bank.docgen.runtime.web;

import com.bank.docgen.runtime.api.AsyncAcceptedResultView;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchGenerateResultView;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.runtime.service.BatchGenerationService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Package-private batch-generate mode branching for runtime template routes.
 */
final class RuntimeTemplateBatchSupport {

    private final BatchGenerationService batchGenerationService;
    private final TraceIdProvider traceIdProvider;

    RuntimeTemplateBatchSupport(BatchGenerationService batchGenerationService, TraceIdProvider traceIdProvider) {
        this.batchGenerationService = batchGenerationService;
        this.traceIdProvider = traceIdProvider;
    }

    ResponseEntity<SuccessEnvelope<?>> batchGenerate(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String environment,
            String releaseVersion,
            String routeType,
            BatchGenerateRequestBody body,
            HttpServletRequest request
    ) {
        if ("ASYNC_TASK".equalsIgnoreCase(body.output().mode())) {
            AsyncAcceptedResultView result = batchGenerationService.batchGenerateAsync(
                    template,
                    session,
                    releaseVersion,
                    routeType,
                    body,
                    environment
            );
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(envelope(request, result));
        }
        BatchGenerateResultView result = batchGenerationService.batchGenerateSync(
                template,
                session,
                environment,
                releaseVersion,
                routeType,
                body,
                traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"))
        );
        return ResponseEntity.ok(envelope(request, result));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
