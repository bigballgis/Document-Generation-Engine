package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.api.AsyncAcceptedResultView;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchGenerateResultView;
import com.bank.docgen.runtime.api.BatchResultItemView;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.api.BatchSummaryView;
import com.bank.docgen.runtime.api.CancelledTaskResultView;
import com.bank.docgen.runtime.api.EncryptionOptionsView;
import com.bank.docgen.runtime.api.EncryptionSummaryView;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.runtime.api.TaskQueryResultView;
import com.bank.docgen.runtime.api.TaskSummaryView;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskRepository;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.service.TemplateValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchGenerationService {

    private final ApiPolicyRepository apiPolicyRepository;
    private final GenerationAsyncTaskRepository asyncTaskRepository;
    private final DocumentGenerationEngine documentGenerationEngine;
    private final IdempotencyService idempotencyService;
    private final AsyncBatchTaskDispatcher asyncBatchTaskDispatcher;
    private final EncryptionParameterValidator encryptionParameterValidator;
    private final ObjectMapper objectMapper;

    public BatchGenerationService(
            ApiPolicyRepository apiPolicyRepository,
            GenerationAsyncTaskRepository asyncTaskRepository,
            DocumentGenerationEngine documentGenerationEngine,
            IdempotencyService idempotencyService,
            AsyncBatchTaskDispatcher asyncBatchTaskDispatcher,
            EncryptionParameterValidator encryptionParameterValidator,
            ObjectMapper objectMapper
    ) {
        this.apiPolicyRepository = apiPolicyRepository;
        this.asyncTaskRepository = asyncTaskRepository;
        this.documentGenerationEngine = documentGenerationEngine;
        this.idempotencyService = idempotencyService;
        this.asyncBatchTaskDispatcher = asyncBatchTaskDispatcher;
        this.encryptionParameterValidator = encryptionParameterValidator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BatchGenerateResultView batchGenerateSync(
            TemplateEntity template,
            RuntimeSessionClaims session,
            String releaseVersion,
            String routeType,
            BatchGenerateRequestBody request
    ) {
        assertTemplateAccess(template, session);
        ApiPolicyEntity policy = requireBatchPolicy(template, request);
        requireSyncMode(request);
        String resolvedVersion = resolveVersion(template, policy, releaseVersion);
        validateBatchRequest(request, policy);

        String requestHash = idempotencyService.hashRequest(writeRequest(request, resolvedVersion));
        Optional<GenerationAsyncTaskEntity> existing = findReplayTask(request, template.getId(), requestHash);
        if (existing.isPresent() && existing.get().getBatchResultJson() != null) {
            return new BatchGenerateResultView(readBatchResult(existing.get().getBatchResultJson()));
        }

        BatchResultView batchResult = executeBatch(template, resolvedVersion, request);
        persistBatchTask(
                template.getId(),
                null,
                batchResult.batchId(),
                TaskStatus.SUCCEEDED,
                routeType,
                resolvedVersion,
                request,
                requestHash,
                batchResult
        );
        return new BatchGenerateResultView(batchResult);
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
        ApiPolicyEntity policy = requireBatchPolicy(template, request);
        requireAsyncMode(request);
        String resolvedVersion = resolveVersion(template, policy, releaseVersion);
        validateBatchRequest(request, policy);

        String requestHash = idempotencyService.hashRequest(writeRequest(request, resolvedVersion));
        Optional<GenerationAsyncTaskEntity> existing = findReplayTask(request, template.getId(), requestHash);
        if (existing.isPresent()) {
            return new AsyncAcceptedResultView(toTaskSummary(existing.get(), template.getExternalId(), environment));
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
                writeRequestPayload(request),
                Instant.now().plusSeconds(86400)
        );
        asyncTaskRepository.save(task);
        asyncBatchTaskDispatcher.dispatch(task.getId());
        GenerationAsyncTaskEntity refreshed = asyncTaskRepository.findById(task.getId()).orElseThrow();
        return new AsyncAcceptedResultView(toTaskSummary(refreshed, template.getExternalId(), environment));
    }

    @Transactional(readOnly = true)
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
        BatchResultView batch = task.getBatchResultJson() == null
                ? null
                : readBatchResult(task.getBatchResultJson());
        return new TaskQueryResultView(toTaskSummary(task, template.getExternalId(), environment), batch);
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
                || task.getStatus() == TaskStatus.CANCELLED
                || task.getStatus() == TaskStatus.EXPIRED) {
            throw new AsyncTaskCancellationNotAllowedException();
        }
        task.markCancelled();
        asyncTaskRepository.save(task);
        return new CancelledTaskResultView(toTaskSummary(task, template.getExternalId(), environment));
    }

    private BatchResultView executeBatch(
            TemplateEntity template,
            String releaseVersion,
            BatchGenerateRequestBody request
    ) {
        String batchId = "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        List<BatchResultItemView> items = new ArrayList<>();
        for (BatchGenerateRequestBody.BatchGenerateItemBody item : request.items()) {
            OutputOptionsView output = item.output() != null ? item.output() : request.output();
            EncryptionOptionsView encryption = item.encryption() != null ? item.encryption() : request.encryption();
            DocumentGenerationEngine.GeneratedDocument generated = documentGenerationEngine.generate(
                    template,
                    releaseVersion,
                    item.variables(),
                    output.format(),
                    encryption
            );
            idempotencyService.registerDownloadableDocument(
                    template.getId(),
                    generated.documentId(),
                    generated.storageKey()
            );
            items.add(new BatchResultItemView(
                    item.itemId(),
                    "SUCCEEDED",
                    output,
                    EncryptionSummaryView.fromRequest(output.format(), encryption),
                    generated.documentId(),
                    generated.fidelityWarningCodes()
            ));
        }
        BatchSummaryView summary = new BatchSummaryView(
                items.size(),
                items.size(),
                items.size(),
                0,
                0
        );
        return new BatchResultView(batchId, summary, items);
    }

    private void persistBatchTask(
            UUID templateId,
            String taskExternalId,
            String batchExternalId,
            TaskStatus status,
            String routeType,
            String releaseVersion,
            BatchGenerateRequestBody request,
            String requestHash,
            BatchResultView batchResult
    ) {
        GenerationAsyncTaskEntity entity = new GenerationAsyncTaskEntity(
                UUID.randomUUID(),
                taskExternalId,
                batchExternalId,
                templateId,
                status,
                routeType,
                releaseVersion,
                request.requestId(),
                request.idempotencyKey(),
                requestHash,
                writeRequestPayload(request),
                Instant.now().plusSeconds(86400)
        );
        entity.markSucceeded(writeBatchResult(batchResult));
        asyncTaskRepository.save(entity);
    }

    private Optional<GenerationAsyncTaskEntity> findReplayTask(
            BatchGenerateRequestBody request,
            UUID templateId,
            String requestHash
    ) {
        return asyncTaskRepository.findByIdempotencyKeyAndTemplateId(request.idempotencyKey(), templateId)
                .filter(task -> task.getRequestHash().equals(requestHash))
                .filter(task -> task.getExpiresAt().isAfter(Instant.now()));
    }

    private ApiPolicyEntity requireBatchPolicy(TemplateEntity template, BatchGenerateRequestBody request) {
        String format = request.output().format();
        if (!"DOCX".equalsIgnoreCase(format) && !"PDF".equalsIgnoreCase(format)) {
            throw new TemplateValidationException("api.error.runtime.outputFormatUnsupported");
        }
        if (!"SYNC_STREAM".equalsIgnoreCase(request.output().mode())
                && !"ASYNC_TASK".equalsIgnoreCase(request.output().mode())) {
            throw new TemplateValidationException("api.error.runtime.outputModeUnsupported");
        }
        ApiPolicyEntity policy = apiPolicyRepository.findByTemplateId(template.getId())
                .orElseThrow(() -> new TemplateValidationException("api.error.runtime.policyNotConfigured"));
        if (readStringList(policy.getOutputFormatsJson()).stream().noneMatch(item -> item.equalsIgnoreCase(format))) {
            throw new TemplateValidationException("api.error.runtime.outputFormatUnsupported");
        }
        if (!policy.isBatchEnabled()) {
            throw new RuntimeBatchValidationException(
                    ApiErrorCodes.OUTPUT_MODE_NOT_ALLOWED,
                    "api.error.runtime.batchNotEnabled"
            );
        }
        return policy;
    }

    private List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void requireSyncMode(BatchGenerateRequestBody request) {
        if (!"SYNC_STREAM".equalsIgnoreCase(request.output().mode())) {
            throw new TemplateValidationException("api.error.runtime.outputModeUnsupported");
        }
    }

    private void requireAsyncMode(BatchGenerateRequestBody request) {
        if (!"ASYNC_TASK".equalsIgnoreCase(request.output().mode())) {
            throw new TemplateValidationException("api.error.runtime.outputModeUnsupported");
        }
    }

    private void validateBatchRequest(BatchGenerateRequestBody request, ApiPolicyEntity policy) {
        encryptionParameterValidator.validate(request.encryption(), policy, request.output().format());
        for (BatchGenerateRequestBody.BatchGenerateItemBody item : request.items()) {
            EncryptionOptionsView itemEncryption = item.encryption() != null ? item.encryption() : request.encryption();
            String outputFormat = item.output() != null ? item.output().format() : request.output().format();
            encryptionParameterValidator.validate(itemEncryption, policy, outputFormat);
        }
        if (request.items().size() > policy.getMaxBatchSize()) {
            throw new RuntimeBatchValidationException(
                    ApiErrorCodes.BATCH_LIMIT_EXCEEDED,
                    "api.error.runtime.batchLimitExceeded"
            );
        }
        Set<String> itemIds = new HashSet<>();
        for (BatchGenerateRequestBody.BatchGenerateItemBody item : request.items()) {
            if (!itemIds.add(item.itemId())) {
                throw new RuntimeBatchValidationException(
                        ApiErrorCodes.ITEM_ID_DUPLICATED,
                        "api.error.runtime.itemIdDuplicated"
                );
            }
        }
    }

    private String resolveVersion(TemplateEntity template, ApiPolicyEntity policy, String releaseVersion) {
        String resolvedVersion = releaseVersion != null ? releaseVersion : policy.getDefaultRouteReleaseVersion();
        if (resolvedVersion == null) {
            throw new TemplateValidationException("api.error.runtime.releaseVersionRequired");
        }
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED
                || !resolvedVersion.equals(template.getReleaseVersion())) {
            throw new TemplateValidationException("api.error.runtime.versionNotCallable");
        }
        return resolvedVersion;
    }

    private void assertTemplateAccess(TemplateEntity template, RuntimeSessionClaims session) {
        if (!template.getId().equals(session.templateId())) {
            throw new TemplateValidationException("api.error.runtime.templateCredentialMismatch");
        }
    }

    private TaskSummaryView toTaskSummary(
            GenerationAsyncTaskEntity task,
            String templateExternalId,
            String environment
    ) {
        String queryPath = task.getTaskExternalId() == null ? null
                : "/api/" + environment + "/v1/templates/" + templateExternalId + "/tasks/" + task.getTaskExternalId();
        return new TaskSummaryView(
                task.getTaskExternalId(),
                task.getStatus().name(),
                queryPath,
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getExpiresAt()
        );
    }

    private String writeBatchResult(BatchResultView batchResult) {
        try {
            return objectMapper.writeValueAsString(batchResult);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.rendering.generationFailed");
        }
    }

    private BatchResultView readBatchResult(String json) {
        try {
            return objectMapper.readValue(json, BatchResultView.class);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.rendering.generationFailed");
        }
    }

    private String writeRequest(BatchGenerateRequestBody request, String releaseVersion) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.of(
                    "releaseVersion", releaseVersion,
                    "items", request.items(),
                    "output", request.output()
            ));
        } catch (JsonProcessingException ex) {
            return releaseVersion;
        }
    }

    private String writeRequestPayload(BatchGenerateRequestBody request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }
}
