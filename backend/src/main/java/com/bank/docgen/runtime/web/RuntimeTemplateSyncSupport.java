package com.bank.docgen.runtime.web;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.api.SyncGenerateResult;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.runtime.service.IdempotencyConstants;
import com.bank.docgen.runtime.service.InvocationRecordService;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Package-private sync-generation invocation recording and HTTP response writing.
 */
final class RuntimeTemplateSyncSupport {

    private final InvocationRecordService invocationRecordService;
    private final ApiPolicyRepository apiPolicyRepository;
    private final TraceIdProvider traceIdProvider;

    RuntimeTemplateSyncSupport(
            InvocationRecordService invocationRecordService,
            ApiPolicyRepository apiPolicyRepository,
            TraceIdProvider traceIdProvider
    ) {
        this.invocationRecordService = invocationRecordService;
        this.apiPolicyRepository = apiPolicyRepository;
        this.traceIdProvider = traceIdProvider;
    }

    String resolveOrRecordSingleInvocation(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String environment,
            String routeType,
            String requestedReleaseVersion,
            SyncGenerateResult result,
            GenerateRequestBody body,
            String auditId
    ) {
        if (IdempotencyConstants.STATUS_REPLAYED.equals(result.idempotencyStatus())) {
            return invocationRecordService.findExistingInvocationId(
                    template.getId(),
                    session.credentialId(),
                    body.idempotencyKey()
            ).orElse(null);
        }
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(template.getId()).orElse(null);
        if (policy == null) {
            return null;
        }
        return invocationRecordService.recordSingleSync(
                template,
                policy,
                session,
                environment,
                routeType,
                requestedReleaseVersion,
                result.resolvedReleaseVersion(),
                body,
                result.documentId(),
                null,
                RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                auditId
        );
    }

    void writeSyncResponse(
            HttpServletRequest request,
            HttpServletResponse response,
            String templateExternalId,
            String routeType,
            GenerateRequestBody body,
            SyncGenerateResult result,
            String invocationId
    ) throws IOException {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(result.contentType());
        response.setHeader("auditId", auditId);
        response.setHeader("traceId", traceId);
        response.setHeader("requestId", body.requestId());
        response.setHeader("idempotencyKey", body.idempotencyKey());
        response.setHeader("idempotencyStatus", result.idempotencyStatus());
        if (invocationId != null) {
            response.setHeader("invocationId", invocationId);
        }
        response.setHeader("documentId", result.documentId());
        response.setHeader("templateId", templateExternalId);
        response.setHeader("routeType", routeType);
        response.setHeader("resolvedReleaseVersion", result.resolvedReleaseVersion());
        response.setHeader("output.format", body.output().format());
        response.setHeader("output.mode", body.output().mode());
        response.setHeader("fidelityWarningCount", String.valueOf(result.fidelityWarningCodes().size()));
        response.setHeader("fidelityWarningCodes", String.join(",", result.fidelityWarningCodes()));
        if (result.artifactStream() != null) {
            try (var artifactStream = result.artifactStream()) {
                artifactStream.transferTo(response.getOutputStream());
            }
            return;
        }
        response.getOutputStream().write(result.artifactBytes());
    }
}
