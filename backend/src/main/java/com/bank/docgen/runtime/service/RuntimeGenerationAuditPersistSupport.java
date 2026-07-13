package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventEntity;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventRepository;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Package-private persistence helpers for runtime generation audit events.
 */
final class RuntimeGenerationAuditPersistSupport {

    static final int SUMMARY_MAX = 512;

    private final RuntimeGenerationAuditEventRepository repository;
    private final TraceIdProvider traceIdProvider;
    private final RuntimeGenerationAuditSpecialPersistSupport special;

    RuntimeGenerationAuditPersistSupport(
            RuntimeGenerationAuditEventRepository repository,
            TraceIdProvider traceIdProvider
    ) {
        this.repository = repository;
        this.traceIdProvider = traceIdProvider;
        this.special = new RuntimeGenerationAuditSpecialPersistSupport(repository, traceIdProvider);
    }

    void persist(
            String eventType,
            TemplateEntity template,
            RuntimeSessionClaims session,
            String environment,
            String routeType,
            String releaseVersion,
            String resolvedReleaseVersion,
            String outputFormat,
            String outputMode,
            String requestId,
            String idempotencyKey,
            String idempotencyStatus,
            String taskExternalId,
            String batchExternalId,
            String documentId,
            String outcome,
            String resultSummary,
            String errorSummary,
            Long durationMs,
            String traceId
    ) {
        persist(
                eventType,
                template,
                session,
                environment,
                routeType,
                releaseVersion,
                resolvedReleaseVersion,
                outputFormat,
                outputMode,
                requestId,
                idempotencyKey,
                idempotencyStatus,
                taskExternalId,
                batchExternalId,
                documentId,
                outcome,
                resultSummary,
                errorSummary,
                durationMs,
                traceId,
                traceIdProvider.newAuditId()
        );
    }

    void persist(
            String eventType,
            TemplateEntity template,
            RuntimeSessionClaims session,
            String environment,
            String routeType,
            String releaseVersion,
            String resolvedReleaseVersion,
            String outputFormat,
            String outputMode,
            String requestId,
            String idempotencyKey,
            String idempotencyStatus,
            String taskExternalId,
            String batchExternalId,
            String documentId,
            String outcome,
            String resultSummary,
            String errorSummary,
            Long durationMs,
            String traceId,
            String auditId
    ) {
        repository.save(new RuntimeGenerationAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                eventType,
                environment,
                template.getId(),
                template.getGroupCode(),
                session.credentialId(),
                fingerprint(session.credentialExternalId()),
                session.accessAccount(),
                releaseVersion,
                resolvedReleaseVersion,
                routeType,
                outputFormat,
                outputMode,
                requestId,
                hashIdempotencyKey(idempotencyKey),
                idempotencyStatus,
                taskExternalId,
                batchExternalId,
                documentId,
                outcome,
                truncate(resultSummary),
                truncate(errorSummary),
                durationMs,
                auditId,
                traceId
        ));
    }

    void persistBatchAsyncCompletedFromTask(
            TemplateEntity template,
            GenerationAsyncTaskEntity task,
            BatchGenerateRequestBody request,
            String outcome,
            String resultSummary,
            String errorSummary
    ) {
        special.persistBatchAsyncCompletedFromTask(template, task, request, outcome, resultSummary, errorSummary);
    }

    void persistRateLimitDenied(
            String environment,
            String credentialExternalId,
            String accessAccount,
            String traceId,
            String auditId
    ) {
        special.persistRateLimitDenied(environment, credentialExternalId, accessAccount, traceId, auditId);
    }

    String currentTraceId() {
        return traceIdProvider.currentOrNew(null);
    }

    String summarize(String value) {
        return truncate(value);
    }

    static String hashIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(idempotencyKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            return null;
        }
    }

    static String fingerprint(String externalId) {
        return externalId == null ? null : "fp-" + externalId;
    }

    static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= SUMMARY_MAX ? value : value.substring(0, SUMMARY_MAX);
    }
}
