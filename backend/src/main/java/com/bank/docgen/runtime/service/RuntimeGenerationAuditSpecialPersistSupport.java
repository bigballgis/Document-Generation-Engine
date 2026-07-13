package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventEntity;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventRepository;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.util.UUID;

/**
 * Package-private special-case runtime audit persists (batch-async-from-task, rate-limit denied).
 */
final class RuntimeGenerationAuditSpecialPersistSupport {

    private final RuntimeGenerationAuditEventRepository repository;
    private final TraceIdProvider traceIdProvider;

    RuntimeGenerationAuditSpecialPersistSupport(
            RuntimeGenerationAuditEventRepository repository,
            TraceIdProvider traceIdProvider
    ) {
        this.repository = repository;
        this.traceIdProvider = traceIdProvider;
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
                java.time.Instant.now(),
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
                RuntimeGenerationAuditPersistSupport.hashIdempotencyKey(request.idempotencyKey()),
                null,
                task.getTaskExternalId(),
                task.getBatchExternalId(),
                null,
                outcome,
                RuntimeGenerationAuditPersistSupport.truncate(resultSummary),
                RuntimeGenerationAuditPersistSupport.truncate(errorSummary),
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
                java.time.Instant.now(),
                RuntimeGenerationAuditRecorder.EVENT_RATE_LIMIT_DENIED,
                environment,
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                null,
                null,
                RuntimeGenerationAuditPersistSupport.fingerprint(credentialExternalId),
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
                RuntimeGenerationAuditPersistSupport.truncate("Rate limit denied"),
                null,
                null,
                auditId,
                traceId
        ));
    }
}
