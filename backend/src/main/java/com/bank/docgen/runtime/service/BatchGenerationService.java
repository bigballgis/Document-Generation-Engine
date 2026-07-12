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
import java.time.Instant;
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
    private final RuntimeGenerationAuditRecorder runtimeGenerationAuditRecorder;
    private final TraceIdProvider traceIdProvider;
    private final InvocationRecordService invocationRecordService;
    private final BatchGenerationJsonSupport json;
    private final BatchGenerationPolicySupport policySupport;
    private final BatchAsyncTaskPersistenceSupport tasks;

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
            InvocationRecordService invocationRecordService
    ) {
        this.asyncTaskRepository = asyncTaskRepository;
        this.idempotencyService = idempotencyService;
        this.asyncBatchTaskDispatcher = asyncBatchTaskDispatcher;
        this.batchExecutionService = batchExecutionService;
        this.runtimeGenerationAuditRecorder = runtimeGenerationAuditRecorder;
        this.traceIdProvider = traceIdProvider;
        this.invocationRecordService = invocationRecordService;
        this.json = new BatchGenerationJsonSupport(objectMapper);
        this.policySupport = new BatchGenerationPolicySupport(
                apiPolicyRepository,
                encryptionParameterValidator,
                templateVersionRepository,
                json
        );
        this.tasks = new BatchAsyncTaskPersistenceSupport(asyncTaskRepository, json);
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

        String requestHash = idempotencyService.hashRequest(json.writeRequest(request, resolvedVersion));
        Optional<GenerationAsyncTaskEntity> existing = tasks.findReplayTask(request, template.getId(), requestHash);
        if (existing.isPresent()) {
            GenerationAsyncTaskEntity replay = existing.get();
            if (replay.getStatus() == TaskStatus.FAILED && replay.getBatchResultJson() != null) {
                throw new SyncBatchFailureException(json.readBatchResult(replay.getBatchResultJson()));
            }
            if (replay.getBatchResultJson() != null) {
                return new BatchGenerateResultView(json.readBatchResult(replay.getBatchResultJson()));
            }
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
            tasks.persistBatchTask(
                    template.getId(),
                    null,
                    outcome.batchResult().batchId(),
                    TaskStatus.SUCCEEDED,
                    routeType,
                    resolvedVersion,
                    request,
                    requestHash,
                    outcome.batchResult()
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
                    outcome.batchResult().batchId(),
                    RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                    "Batch succeeded",
                    null,
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
                    outcome.batchResult(),
                    RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                    auditId
            );
            return new BatchGenerateResultView(outcome.batchResult());
        } catch (SyncBatchFailureException ex) {
            tasks.persistBatchTask(
                    template.getId(),
                    null,
                    ex.batchResult().batchId(),
                    TaskStatus.FAILED,
                    routeType,
                    resolvedVersion,
                    request,
                    requestHash,
                    ex.batchResult()
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
                    ex.batchResult().batchId(),
                    RuntimeGenerationAuditRecorder.OUTCOME_FAILURE,
                    "Batch failed",
                    ex.batchResult().batchId(),
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
                    ex.batchResult(),
                    RuntimeGenerationAuditRecorder.OUTCOME_FAILURE,
                    auditId
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

        String requestHash = idempotencyService.hashRequest(json.writeRequest(request, resolvedVersion));
        Optional<GenerationAsyncTaskEntity> existing = tasks.findReplayTask(request, template.getId(), requestHash);
        if (existing.isPresent()) {
            return new AsyncAcceptedResultView(tasks.toTaskSummary(existing.get(), template.getExternalId(), environment));
        }

        String taskId = "TASK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        String batchId = "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        GenerationAsyncTaskEntity task = new GenerationAsyncTaskEntity(
                UUID.randomUUID(),
                taskId,
                batchId,
                template.getId(),
                TaskStatus.ACCEPTED,
                routeType,
                resolvedVersion,
                request.requestId(),
                request.idempotencyKey(),
                requestHash,
                json.writeRequestPayload(request),
                Instant.now().plusSeconds(IdempotencyConstants.RETENTION_SECONDS)
        );
        asyncTaskRepository.save(task);
        asyncBatchTaskDispatcher.dispatch(task.getId());
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
        if (task.getStatus() == TaskStatus.SUCCEEDED
                || task.getStatus() == TaskStatus.FAILED
                || task.getStatus() == TaskStatus.PARTIAL_SUCCEEDED
                || task.getStatus() == TaskStatus.CANCELLED
                || task.getStatus() == TaskStatus.EXPIRED) {
            throw new AsyncTaskCancellationNotAllowedException();
        }
        task.markCancelled();
        asyncTaskRepository.save(task);
        return new CancelledTaskResultView(tasks.toTaskSummary(task, template.getExternalId(), environment));
    }

    private void assertTemplateAccess(TemplateEntity template, RuntimeSessionClaims session) {
        if (!template.getId().equals(session.templateId())) {
            throw new TemplateValidationException("api.error.runtime.templateCredentialMismatch");
        }
    }
}
