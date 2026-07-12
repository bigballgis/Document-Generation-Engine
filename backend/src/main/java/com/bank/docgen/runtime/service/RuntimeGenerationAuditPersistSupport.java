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

    private static final int SUMMARY_MAX = 512;

    private final RuntimeGenerationAuditEventRepository repository;
    private final TraceIdProvider traceIdProvider;

    RuntimeGenerationAuditPersistSupport(
            RuntimeGenerationAuditEventRepository repository,
            TraceIdProvider traceIdProvider
    ) {
        this.repository = repository;
        this.traceIdProvider = traceIdProvider;
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
        repository.save(new RuntimeGenerationAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                RuntimeGenerationAuditRecorder.EVENT_BATCH_ASYNC_COMPLETED,
                RuntimeGenerationAuditRecorder.ASYNC_ENVIRONMENT,
                template.getId(),
                template.getGroupCode(),
                null,
                null,
                null,
                task.getReleaseVersion(),
                task.getReleaseVersion(),
                task.getRouteType(),
                request.output().format(),
                request.output().mode(),
                request.requestId(),
                hashIdempotencyKey(request.idempotencyKey()),
                null,
                task.getTaskExternalId(),
                task.getBatchExternalId(),
                null,
                outcome,
                truncate(resultSummary),
                truncate(errorSummary),
                null,
                traceIdProvider.newAuditId(),
                traceIdProvider.currentOrNew(null)
        ));
    }

    void persistRateLimitDenied(
            String environment,
            String credentialExternalId,
            String accessAccount,
            String traceId,
            String auditId
    ) {
        repository.save(new RuntimeGenerationAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                RuntimeGenerationAuditRecorder.EVENT_RATE_LIMIT_DENIED,
                environment,
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                null,
                null,
                fingerprint(credentialExternalId),
                accessAccount,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                RuntimeGenerationAuditRecorder.OUTCOME_FAILURE,
                summarize("Rate limit denied"),
                null,
                null,
                auditId,
                traceId
        ));
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

    private String fingerprint(String externalId) {
        return externalId == null ? null : "fp-" + externalId;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= SUMMARY_MAX ? value : value.substring(0, SUMMARY_MAX);
    }
}
