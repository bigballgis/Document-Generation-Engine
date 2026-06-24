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
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuntimeGenerationAuditRecorder {

    public static final String EVENT_SYNC_GENERATION = "API_GENERATION_SYNC";
    public static final String EVENT_BATCH_SYNC = "API_GENERATION_BATCH_SYNC";
    public static final String EVENT_BATCH_ASYNC_ACCEPTED = "API_GENERATION_BATCH_ASYNC_ACCEPTED";
    public static final String EVENT_BATCH_ASYNC_COMPLETED = "API_GENERATION_BATCH_ASYNC_COMPLETED";
    public static final String EVENT_DOCUMENT_DOWNLOAD = "API_DOCUMENT_DOWNLOAD";

    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_FAILURE = "FAILURE";
    public static final String OUTCOME_REPLAYED = "REPLAYED";
    public static final String ASYNC_ENVIRONMENT = "async";

    private static final int SUMMARY_MAX = 512;

    private final RuntimeGenerationAuditEventRepository repository;
    private final TraceIdProvider traceIdProvider;

    public RuntimeGenerationAuditRecorder(
            RuntimeGenerationAuditEventRepository repository,
            TraceIdProvider traceIdProvider
    ) {
        this.repository = repository;
        this.traceIdProvider = traceIdProvider;
    }

    @Transactional
    public void recordSyncGeneration(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String environment,
            String routeType,
            String resolvedReleaseVersion,
            String outputFormat,
            String outputMode,
            String requestId,
            String idempotencyKey,
            String idempotencyStatus,
            String documentId,
            String outcome,
            String traceId
    ) {
        persist(
                EVENT_SYNC_GENERATION,
                template,
                session,
                environment,
                routeType,
                resolvedReleaseVersion,
                resolvedReleaseVersion,
                outputFormat,
                outputMode,
                requestId,
                idempotencyKey,
                idempotencyStatus,
                null,
                null,
                documentId,
                outcome,
                summarize("Sync generation " + outcome.toLowerCase(Locale.ROOT)),
                null,
                null,
                traceId
        );
    }

    @Transactional
    public void recordBatchSync(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String environment,
            String routeType,
            String resolvedReleaseVersion,
            String outputFormat,
            String outputMode,
            String requestId,
            String idempotencyKey,
            String batchExternalId,
            String outcome,
            String resultSummary,
            String errorSummary,
            String traceId
    ) {
        persist(
                EVENT_BATCH_SYNC,
                template,
                session,
                environment,
                routeType,
                resolvedReleaseVersion,
                resolvedReleaseVersion,
                outputFormat,
                outputMode,
                requestId,
                idempotencyKey,
                null,
                null,
                batchExternalId,
                null,
                outcome,
                resultSummary,
                errorSummary,
                null,
                traceId
        );
    }

    @Transactional
    public void recordBatchAsyncAccepted(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String environment,
            String routeType,
            String resolvedReleaseVersion,
            String outputFormat,
            String outputMode,
            String requestId,
            String idempotencyKey,
            String taskExternalId,
            String batchExternalId,
            String traceId
    ) {
        persist(
                EVENT_BATCH_ASYNC_ACCEPTED,
                template,
                session,
                environment,
                routeType,
                resolvedReleaseVersion,
                resolvedReleaseVersion,
                outputFormat,
                outputMode,
                requestId,
                idempotencyKey,
                null,
                taskExternalId,
                batchExternalId,
                null,
                OUTCOME_SUCCESS,
                summarize("Async batch accepted"),
                null,
                null,
                traceId
        );
    }

    @Transactional
    public void recordBatchAsyncCompleted(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String environment,
            String routeType,
            String resolvedReleaseVersion,
            String outputFormat,
            String outputMode,
            String requestId,
            String idempotencyKey,
            String taskExternalId,
            String batchExternalId,
            String outcome,
            String resultSummary,
            String errorSummary
    ) {
        persist(
                EVENT_BATCH_ASYNC_COMPLETED,
                template,
                session,
                environment,
                routeType,
                resolvedReleaseVersion,
                resolvedReleaseVersion,
                outputFormat,
                outputMode,
                requestId,
                idempotencyKey,
                null,
                taskExternalId,
                batchExternalId,
                null,
                outcome,
                resultSummary,
                errorSummary,
                null,
                traceIdProvider.currentOrNew(null)
        );
    }

    @Transactional
    public void recordBatchAsyncCompletedFromTask(
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
                EVENT_BATCH_ASYNC_COMPLETED,
                ASYNC_ENVIRONMENT,
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

    @Transactional
    public void recordDocumentDownload(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String environment,
            String documentId,
            String auditId,
            String traceId
    ) {
        persist(
                EVENT_DOCUMENT_DOWNLOAD,
                template,
                session,
                environment,
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
                documentId,
                OUTCOME_SUCCESS,
                summarize("Document download"),
                null,
                null,
                traceId,
                auditId
        );
    }

    private void persist(
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

    private void persist(
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

    private String summarize(String value) {
        return truncate(value);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= SUMMARY_MAX ? value : value.substring(0, SUMMARY_MAX);
    }
}
