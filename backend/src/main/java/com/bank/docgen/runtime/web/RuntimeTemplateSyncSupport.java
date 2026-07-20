package com.bank.docgen.runtime.web;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.api.SyncGenerateResult;
import com.bank.docgen.runtime.domain.InvocationErrorEnvelope;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.runtime.service.IdempotencyConstants;
import com.bank.docgen.runtime.service.InvocationRecordService;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Package-private sync-generation audit, invocation recording, and HTTP response writing.
 */
final class RuntimeTemplateSyncSupport {

    private static final Logger LOG = LoggerFactory.getLogger(RuntimeTemplateSyncSupport.class);

    private final InvocationRecordService invocationRecordService;
    private final ApiPolicyRepository apiPolicyRepository;
    private final TraceIdProvider traceIdProvider;
    private final RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder;
    private final MessageResolver messageResolver;

    RuntimeTemplateSyncSupport(
            InvocationRecordService invocationRecordService,
            ApiPolicyRepository apiPolicyRepository,
            TraceIdProvider traceIdProvider,
            RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder,
            MessageResolver messageResolver
    ) {
        this.invocationRecordService = invocationRecordService;
        this.apiPolicyRepository = apiPolicyRepository;
        this.traceIdProvider = traceIdProvider;
        this.runtimeGenerationAuditRecorder = runtimeGenerationAuditRecorder;
        this.messageResolver = messageResolver;
    }

    void auditRecordAndWrite(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String environment,
            String routeType,
            String requestedReleaseVersion,
            String templateExternalId,
            GenerateRequestBody body,
            SyncGenerateResult result,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        runtimeGenerationAuditRecorder.recordSyncGeneration(
                template,
                session,
                environment,
                routeType,
                result.resolvedReleaseVersion(),
                body.output().format(),
                body.output().mode(),
                body.requestId(),
                body.idempotencyKey(),
                result.idempotencyStatus(),
                result.documentId(),
                IdempotencyConstants.STATUS_REPLAYED.equals(result.idempotencyStatus())
                        ? RuntimeGenerationAuditRecorder.OUTCOME_REPLAYED
                        : RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                traceId
        );
        String invocationId = resolveOrRecordSingleInvocation(
                template,
                session,
                environment,
                routeType,
                requestedReleaseVersion,
                result,
                body,
                auditId
        );
        writeSyncResponse(request, response, templateExternalId, routeType, body, result, invocationId);
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

    /**
     * Persists a failed single-sync invocation with the platform error envelope (CE-U11 IRC-006).
     * Best-effort: recording failures must not mask the original generate exception.
     */
    String recordFailedSingleInvocation(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String environment,
            String routeType,
            String requestedReleaseVersion,
            GenerateRequestBody body,
            Throwable failure
    ) {
        InvocationErrorEnvelope errorEnvelope = FailedSyncInvocationErrorMapper.from(failure, messageResolver);
        if (errorEnvelope == null) {
            return null;
        }
        try {
            ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(template.getId()).orElse(null);
            if (policy == null) {
                return null;
            }
            String auditId = traceIdProvider.newAuditId();
            String resolvedReleaseVersion = requestedReleaseVersion;
            return invocationRecordService.recordSingleSync(
                    template,
                    policy,
                    session,
                    environment,
                    routeType,
                    requestedReleaseVersion,
                    resolvedReleaseVersion,
                    body,
                    null,
                    null,
                    RuntimeGenerationAuditRecorder.OUTCOME_FAILURE,
                    auditId,
                    errorEnvelope
            );
        } catch (RuntimeException recordingFailure) {
            LOG.warn(
                    "Failed to persist failed invocation record for template {}: {}",
                    template.getExternalId(),
                    recordingFailure.getMessage()
            );
            return null;
        }
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
        if (result.resolvedLegalEntityCode() != null && !result.resolvedLegalEntityCode().isBlank()) {
            response.setHeader("resolvedLegalEntityCode", result.resolvedLegalEntityCode());
        }
        if (result.resolvedDocumentBrandCode() != null && !result.resolvedDocumentBrandCode().isBlank()) {
            response.setHeader("resolvedDocumentBrandCode", result.resolvedDocumentBrandCode());
        }
        if (result.artifactStream() != null) {
            try (var artifactStream = result.artifactStream()) {
                artifactStream.transferTo(response.getOutputStream());
            }
            return;
        }
        response.getOutputStream().write(result.artifactBytes());
    }
}
