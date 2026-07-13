package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;

/**
 * Package-private sync/async outcome audit + invocation recording for batch generation.
 */
final class BatchGenerationOutcomeSupport {

    private final RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder;
    private final InvocationRecordService invocationRecordService;
    private final TraceIdProvider traceIdProvider;
    private final BatchAsyncTaskPersistenceSupport tasks;

    BatchGenerationOutcomeSupport(
            RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder,
            InvocationRecordService invocationRecordService,
            TraceIdProvider traceIdProvider,
            BatchAsyncTaskPersistenceSupport tasks
    ) {
        this.runtimeGenerationAuditRecorder = runtimeGenerationAuditRecorder;
        this.invocationRecordService = invocationRecordService;
        this.traceIdProvider = traceIdProvider;
        this.tasks = tasks;
    }

    void persistAndRecordSync(
            TemplateEntity template,
            ApiPolicyEntity policy,
            RuntimeSessionClaims session,
            String environment,
            String routeType,
            String releaseVersion,
            String resolvedVersion,
            BatchGenerateRequestBody request,
            String requestHash,
            BatchResultView batchResult,
            TaskStatus status,
            String outcome,
            String message,
            String failureRef,
            String traceId,
            String auditId
    ) {
        tasks.persistBatchTask(
                template.getId(),
                null,
                batchResult.batchId(),
                status,
                routeType,
                resolvedVersion,
                request,
                requestHash,
                batchResult
        );
        runtimeGenerationAuditRecorder.recordBatchSync(
                template,
                session,
                environment,
                routeType,
                resolvedVersion,
                request.output().format(),
                request.output().mode(),
                request.requestId(),
                request.idempotencyKey(),
                batchResult.batchId(),
                outcome,
                message,
                failureRef,
                traceId
        );
        invocationRecordService.recordBatchSync(
                template,
                policy,
                session,
                environment,
                routeType,
                releaseVersion,
                resolvedVersion,
                request,
                batchResult,
                outcome,
                auditId
        );
    }

    void recordAsyncAccepted(
            TemplateEntity template,
            ApiPolicyEntity policy,
            RuntimeSessionClaims session,
            String environment,
            String routeType,
            String releaseVersion,
            String resolvedVersion,
            BatchGenerateRequestBody request,
            String taskId,
            String batchId
    ) {
        String auditId = traceIdProvider.newAuditId();
        runtimeGenerationAuditRecorder.recordBatchAsyncAccepted(
                template,
                session,
                environment,
                routeType,
                resolvedVersion,
                request.output().format(),
                request.output().mode(),
                request.requestId(),
                request.idempotencyKey(),
                taskId,
                batchId,
                traceIdProvider.currentOrNew(null)
        );
        invocationRecordService.recordAsyncAccepted(
                template,
                policy,
                session,
                environment,
                routeType,
                releaseVersion,
                resolvedVersion,
                request,
                taskId,
                batchId,
                auditId
        );
    }
}
