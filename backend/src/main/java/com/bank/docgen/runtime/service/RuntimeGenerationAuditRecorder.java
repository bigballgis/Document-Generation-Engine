package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventRepository;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuntimeGenerationAuditRecorder {

    public static final String EVENT_SYNC_GENERATION = "API_GENERATION_SYNC";
    public static final String EVENT_BATCH_SYNC = "API_GENERATION_BATCH_SYNC";
    public static final String EVENT_BATCH_ASYNC_ACCEPTED = "API_GENERATION_BATCH_ASYNC_ACCEPTED";
    public static final String EVENT_BATCH_ASYNC_COMPLETED = "API_GENERATION_BATCH_ASYNC_COMPLETED";
    public static final String EVENT_DOCUMENT_DOWNLOAD = "API_DOCUMENT_DOWNLOAD";
    public static final String EVENT_RATE_LIMIT_DENIED = "API_RATE_LIMIT_DENIED";

    public static final String OUTCOME_SUCCESS = "SUCCESS";
    public static final String OUTCOME_FAILURE = "FAILURE";
    public static final String OUTCOME_REPLAYED = "REPLAYED";
    public static final String ASYNC_ENVIRONMENT = "async";

    private final RuntimeGenerationAuditPersistSupport persist;
    private final TraceIdProvider traceIdProvider;

    public RuntimeGenerationAuditRecorder(
            RuntimeGenerationAuditEventRepository repository,
            TraceIdProvider traceIdProvider
    ) {
        this.traceIdProvider = traceIdProvider;
        this.persist = new RuntimeGenerationAuditPersistSupport(repository, traceIdProvider);
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
        persist.persist(
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
                persist.summarize("Sync generation " + outcome.toLowerCase(Locale.ROOT)),
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
        persist.persist(
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
        persist.persist(
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
                persist.summarize("Async batch accepted"),
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
        persist.persist(
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
        persist.persistBatchAsyncCompletedFromTask(template, task, request, outcome, resultSummary, errorSummary);
    }

    @Transactional
    public void recordRateLimitDenied(
            String environment,
            String credentialExternalId,
            String accessAccount,
            String traceId,
            String auditId
    ) {
        persist.persistRateLimitDenied(environment, credentialExternalId, accessAccount, traceId, auditId);
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
        persist.persist(
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
                persist.summarize("Document download"),
                null,
                null,
                traceId,
                auditId
        );
    }

    static String hashIdempotencyKey(String idempotencyKey) {
        return RuntimeGenerationAuditPersistSupport.hashIdempotencyKey(idempotencyKey);
    }
}
