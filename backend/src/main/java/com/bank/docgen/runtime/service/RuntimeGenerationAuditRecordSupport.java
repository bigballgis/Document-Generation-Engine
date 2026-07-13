package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventRepository;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.util.Locale;

/**
 * Package-private typed record helpers layered on {@link RuntimeGenerationAuditPersistSupport}.
 */
final class RuntimeGenerationAuditRecordSupport {

    private final RuntimeGenerationAuditPersistSupport persist;

    RuntimeGenerationAuditRecordSupport(
            RuntimeGenerationAuditEventRepository repository,
            TraceIdProvider traceIdProvider
    ) {
        this.persist = new RuntimeGenerationAuditPersistSupport(repository, traceIdProvider);
    }

    RuntimeGenerationAuditPersistSupport persist() {
        return persist;
    }

    void persistSyncGeneration(
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
        persist.persist(
                RuntimeGenerationAuditRecorder.EVENT_SYNC_GENERATION,
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
                persist.summarize("Sync generation " + outcome.toLowerCase(Locale.ROOT)),
                null,
                null,
                traceId
        );
    }

    void persistBatchSync(
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
        persist.persist(
                RuntimeGenerationAuditRecorder.EVENT_BATCH_SYNC,
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

    void persistBatchAsyncAccepted(
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
        persist.persist(
                RuntimeGenerationAuditRecorder.EVENT_BATCH_ASYNC_ACCEPTED,
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
                RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                persist.summarize("Async batch accepted"),
                null,
                null,
                traceId
        );
    }

    void persistBatchAsyncCompleted(
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
        persist.persist(
                RuntimeGenerationAuditRecorder.EVENT_BATCH_ASYNC_COMPLETED,
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
                persist.currentTraceId()
        );
    }

    void persistDocumentDownload(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String environment,
            String documentId,
            String auditId,
            String traceId
    ) {
        persist.persist(
                RuntimeGenerationAuditRecorder.EVENT_DOCUMENT_DOWNLOAD,
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
                RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                persist.summarize("Document download"),
                null,
                null,
                traceId,
                auditId
        );
    }
}
