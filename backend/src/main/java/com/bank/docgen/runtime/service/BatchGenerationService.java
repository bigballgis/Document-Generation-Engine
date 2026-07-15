package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.api.AsyncAcceptedResultView;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchGenerateResultView;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.api.CancelledTaskResultView;
import com.bank.docgen.runtime.api.TaskQueryResultView;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskRepository;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchGenerationService {

    private final GenerationAsyncTaskRepository asyncTaskRepository;
    private final IdempotencyService idempotencyService;
    private final AsyncBatchTaskDispatcher asyncBatchTaskDispatcher;
    private final BatchExecutionService batchExecutionService;
    private final TraceIdProvider traceIdProvider;
    private final BatchGenerationJsonSupport json;
    private final BatchGenerationPolicySupport policySupport;
    private final BatchAsyncTaskPersistenceSupport tasks;
    private final BatchGenerationOutcomeSupport outcomes;
    private final OriginalBatchLineageValidator originalBatchLineageValidator;

    public BatchGenerationService(
            ApiPolicyRepository apiPolicyRepository,
            GenerationAsyncTaskRepository asyncTaskRepository,
            IdempotencyService idempotencyService,
            AsyncBatchTaskDispatcher asyncBatchTaskDispatcher,
            EncryptionParameterValidator encryptionParameterValidator,
            ObjectMapper objectMapper,
            TemplateVersionRepository templateVersionRepository,
            BatchExecutionService batchExecutionService,
            RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder,
            TraceIdProvider traceIdProvider,
            InvocationRecordService invocationRecordService,
            OriginalBatchLineageValidator originalBatchLineageValidator
    ) {
        this.asyncTaskRepository = asyncTaskRepository;
        this.idempotencyService = idempotencyService;
        this.asyncBatchTaskDispatcher = asyncBatchTaskDispatcher;
        this.batchExecutionService = batchExecutionService;
        this.traceIdProvider = traceIdProvider;
        this.json = new BatchGenerationJsonSupport(objectMapper);
        this.policySupport = new BatchGenerationPolicySupport(
                apiPolicyRepository,
                encryptionParameterValidator,
                templateVersionRepository,
                json
        );
        this.tasks = new BatchAsyncTaskPersistenceSupport(asyncTaskRepository, json);
        this.outcomes = new BatchGenerationOutcomeSupport(
                runtimeGenerationAuditRecorder,
                invocationRecordService,
                traceIdProvider,
                tasks
        );
        this.originalBatchLineageValidator = originalBatchLineageValidator;
    }

    @Transactional
    public BatchGenerateResultView batchGenerateSync(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String environment,
            String releaseVersion,
            String routeType,
            BatchGenerateRequestBody request,
            String traceId
    ) {
        assertTemplateAccess(template, session);
        ApiPolicyEntity policy = policySupport.requireBatchPolicy(template, request);
        policySupport.requireSyncMode(request, policy);
        String resolvedVersion = policySupport.resolveVersion(template, policy, releaseVersion);
        policySupport.validateBatchRequest(request, policy);
        originalBatchLineageValidator.requireValidOriginalBatchIfPresent(
                request.originalBatchId(),
                session.credentialId()
        );

        String requestHash = idempotencyService.hashRequest(json.writeRequest(request, resolvedVersion));
        Optional<BatchGenerateResultView> replay =
                tasks.resolveSyncReplay(tasks.findReplayTask(request, template.getId(), requestHash));
        if (replay.isPresent()) {
            return replay.get();
        }

        String auditId = traceIdProvider.newAuditId();
        try {
            BatchExecutionService.BatchExecutionOutcome outcome = batchExecutionService.execute(
                    template,
                    resolvedVersion,
                    request,
                    "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT),
                    false
            );
            outcomes.persistAndRecordSync(
                    template, policy, session, environment, routeType, releaseVersion, resolvedVersion,
                    request, requestHash, outcome.batchResult(), TaskStatus.SUCCEEDED,
                    RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS, "Batch succeeded", null, traceId, auditId
            );
            return new BatchGenerateResultView(outcome.batchResult());
        } catch (SyncBatchFailureException ex) {
            outcomes.persistAndRecordSync(
                    template, policy, session, environment, routeType, releaseVersion, resolvedVersion,
                    request, requestHash, ex.batchResult(), TaskStatus.FAILED,
                    RuntimeGenerationAuditRecorder.OUTCOME_FAILURE, "Batch failed",
                    ex.batchResult().batchId(), traceId, auditId
            );
            throw ex;
        }
    }

    @Transactional
    public AsyncAcceptedResultView batchGenerateAsync(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String releaseVersion,
            String routeType,
            BatchGenerateRequestBody request,
            String environment
    ) {
        assertTemplateAccess(template, session);
        ApiPolicyEntity policy = policySupport.requireBatchPolicy(template, request);
        policySupport.requireAsyncMode(request, policy);
        String resolvedVersion = policySupport.resolveVersion(template, policy, releaseVersion);
        policySupport.validateBatchRequest(request, policy);
        originalBatchLineageValidator.requireValidOriginalBatchIfPresent(
                request.originalBatchId(),
                session.credentialId()
        );

        String requestHash = idempotencyService.hashRequest(json.writeRequest(request, resolvedVersion));
        Optional<GenerationAsyncTaskEntity> existing = tasks.findReplayTask(request, template.getId(), requestHash);
        if (existing.isPresent()) {
            return new AsyncAcceptedResultView(tasks.toTaskSummary(existing.get(), template.getExternalId(), environment));
        }

        GenerationAsyncTaskEntity task = tasks.createAcceptedTask(
                template, routeType, resolvedVersion, request, requestHash, asyncBatchTaskDispatcher);
        outcomes.recordAsyncAccepted(
                template, policy, session, environment, routeType, releaseVersion, resolvedVersion,
                request, task.getTaskExternalId(), task.getBatchExternalId()
        );
        GenerationAsyncTaskEntity refreshed = asyncTaskRepository.findById(task.getId()).orElseThrow();
        return new AsyncAcceptedResultView(tasks.toTaskSummary(refreshed, template.getExternalId(), environment));
    }

    @Transactional
    public TaskQueryResultView getTask(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String taskId,
            String environment
    ) {
        assertTemplateAccess(template, session);
        GenerationAsyncTaskEntity task = asyncTaskRepository
                .findByTaskExternalIdAndTemplateId(taskId, template.getId())
                .orElseThrow(AsyncTaskNotFoundException::new);
        tasks.ensureTaskQueryable(task);
        BatchResultView batch = task.getBatchResultJson() == null
                ? null
                : json.readBatchResult(task.getBatchResultJson());
        return new TaskQueryResultView(tasks.toTaskSummary(task, template.getExternalId(), environment), batch);
    }

    @Transactional
    public CancelledTaskResultView cancelTask(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String taskId,
            String environment
    ) {
        assertTemplateAccess(template, session);
        GenerationAsyncTaskEntity task = asyncTaskRepository
                .findByTaskExternalIdAndTemplateId(taskId, template.getId())
                .orElseThrow(AsyncTaskNotFoundException::new);
        if (isTerminal(task.getStatus())) {
            throw new AsyncTaskCancellationNotAllowedException();
        }
        task.markCancelled();
        asyncTaskRepository.save(task);
        return new CancelledTaskResultView(tasks.toTaskSummary(task, template.getExternalId(), environment));
    }

    private static boolean isTerminal(TaskStatus status) {
        return status == TaskStatus.SUCCEEDED
                || status == TaskStatus.FAILED
                || status == TaskStatus.PARTIAL_SUCCEEDED
                || status == TaskStatus.CANCELLED
                || status == TaskStatus.EXPIRED;
    }

    private void assertTemplateAccess(TemplateEntity template, RuntimeSessionClaims session) {
        if (!template.getId().equals(session.templateId())) {
            throw new TemplateValidationException("api.error.runtime.templateCredentialMismatch");
        }
    }
}
