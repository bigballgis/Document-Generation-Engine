package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultItemView;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationStatus;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvocationRecordService {

    private static final List<InvocationKind> ROOT_INVOCATION_KINDS = List.of(
            InvocationKind.SINGLE,
            InvocationKind.BATCH_ROOT,
            InvocationKind.ASYNC_TASK
    );

    private final ApiInvocationRecordRepository repository;
    private final InvocationParameterSanitizer parameterSanitizer;
    private final IdempotencyService idempotencyService;

    public InvocationRecordService(
            ApiInvocationRecordRepository repository,
            InvocationParameterSanitizer parameterSanitizer,
            IdempotencyService idempotencyService
    ) {
        this.repository = repository;
        this.parameterSanitizer = parameterSanitizer;
        this.idempotencyService = idempotencyService;
    }

    @Transactional(readOnly = true)
    public Optional<String> findExistingInvocationId(
            UUID templateId,
            UUID credentialId,
            String idempotencyKey
    ) {
        return findLiveRootRecord(templateId, credentialId, idempotencyKey)
                .map(ApiInvocationRecordEntity::getInvocationExternalId);
    }

    @Transactional
    public String recordSingleSync(
            TemplateEntity template,
            ApiPolicyEntity policy,
            RuntimeSessionClaims session,
            String environment,
            String routeType,
            String requestedReleaseVersion,
            String resolvedReleaseVersion,
            GenerateRequestBody request,
            String documentId,
            String artifactStorageKey,
            String outcome,
            String auditId
    ) {
        Instant now = Instant.now();
        boolean hasArtifact = documentId != null && !documentId.isBlank();
        UUID idempotencyRecordId = resolveIdempotencyRecordId(request.idempotencyKey(), template.getId());
        String resolvedArtifactStorageKey = artifactStorageKey;
        if (resolvedArtifactStorageKey == null && hasArtifact) {
            resolvedArtifactStorageKey = idempotencyService.findLiveRecord(request.idempotencyKey(), template.getId())
                    .map(GenerationIdempotencyEntity::getResponseStorageKey)
                    .orElse(null);
        }
        boolean artifactSaved = policy.isSaveGeneratedDocuments() && hasArtifact;
        ApiInvocationRecordEntity entity = new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                newInvocationExternalId(),
                InvocationKind.SINGLE,
                mapOutcomeToStatus(outcome),
                environment,
                template.getId(),
                template.getExternalId(),
                session.credentialId(),
                session.accessAccount(),
                request.requestId(),
                request.idempotencyKey(),
                routeType,
                requestedReleaseVersion,
                resolvedReleaseVersion,
                request.output().format(),
                request.output().mode(),
                outcome,
                null,
                parameterSanitizer.sanitizeSingleRequest(request, resolvedReleaseVersion),
                documentId,
                artifactSaved ? resolvedArtifactStorageKey : null,
                artifactSaved,
                recordExpiresAt(policy, now),
                documentExpiresAt(policy, artifactSaved, now),
                null,
                null,
                null,
                null,
                idempotencyRecordId,
                auditId,
                false,
                now,
                now
        );
        repository.save(entity);
        return entity.getInvocationExternalId();
    }

    @Transactional
    public String recordBatchSync(
            TemplateEntity template,
            ApiPolicyEntity policy,
            RuntimeSessionClaims session,
            String environment,
            String routeType,
            String requestedReleaseVersion,
            String resolvedReleaseVersion,
            BatchGenerateRequestBody request,
            BatchResultView batchResult,
            String outcome,
            String auditId
    ) {
        Instant now = Instant.now();
        UUID idempotencyRecordId = resolveIdempotencyRecordId(request.idempotencyKey(), template.getId());
        String rootInvocationId = newInvocationExternalId();
        InvocationStatus rootStatus = mapBatchRootStatus(batchResult);
        ApiInvocationRecordEntity root = new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                rootInvocationId,
                InvocationKind.BATCH_ROOT,
                rootStatus,
                environment,
                template.getId(),
                template.getExternalId(),
                session.credentialId(),
                session.accessAccount(),
                request.requestId(),
                request.idempotencyKey(),
                routeType,
                requestedReleaseVersion,
                resolvedReleaseVersion,
                request.output().format(),
                request.output().mode(),
                outcome,
                null,
                parameterSanitizer.sanitizeBatchRequest(request, resolvedReleaseVersion),
                null,
                null,
                false,
                recordExpiresAt(policy, now),
                null,
                batchResult.batchId(),
                null,
                null,
                null,
                idempotencyRecordId,
                auditId,
                true,
                now,
                now
        );
        repository.save(root);
        persistBatchItems(
                template,
                policy,
                session.credentialId(),
                session.accessAccount(),
                environment,
                routeType,
                requestedReleaseVersion,
                resolvedReleaseVersion,
                request,
                batchResult,
                rootInvocationId,
                batchResult.batchId(),
                null,
                idempotencyRecordId,
                auditId,
                now
        );
        return rootInvocationId;
    }

    @Transactional
    public String recordAsyncAccepted(
            TemplateEntity template,
            ApiPolicyEntity policy,
            RuntimeSessionClaims session,
            String environment,
            String routeType,
            String requestedReleaseVersion,
            String resolvedReleaseVersion,
            BatchGenerateRequestBody request,
            String taskExternalId,
            String batchExternalId,
            String auditId
    ) {
        Instant now = Instant.now();
        UUID idempotencyRecordId = resolveIdempotencyRecordId(request.idempotencyKey(), template.getId());
        ApiInvocationRecordEntity entity = new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                newInvocationExternalId(),
                InvocationKind.ASYNC_TASK,
                InvocationStatus.ACCEPTED,
                environment,
                template.getId(),
                template.getExternalId(),
                session.credentialId(),
                session.accessAccount(),
                request.requestId(),
                request.idempotencyKey(),
                routeType,
                requestedReleaseVersion,
                resolvedReleaseVersion,
                request.output().format(),
                request.output().mode(),
                RuntimeGenerationAuditRecorder.OUTCOME_SUCCESS,
                null,
                parameterSanitizer.sanitizeBatchRequest(request, resolvedReleaseVersion),
                null,
                null,
                false,
                recordExpiresAt(policy, now),
                null,
                batchExternalId,
                null,
                null,
                taskExternalId,
                idempotencyRecordId,
                auditId,
                true,
                now,
                now
        );
        repository.save(entity);
        return entity.getInvocationExternalId();
    }

    @Transactional
    public void completeAsyncBatch(
            TemplateEntity template,
            ApiPolicyEntity policy,
            String environment,
            String routeType,
            String requestedReleaseVersion,
            String resolvedReleaseVersion,
            BatchGenerateRequestBody request,
            GenerationAsyncTaskEntity task,
            BatchResultView batchResult,
            TaskStatus taskStatus,
            String outcome,
            String auditId
    ) {
        Instant now = Instant.now();
        Optional<ApiInvocationRecordEntity> asyncRecord = repository.findByTaskExternalId(task.getTaskExternalId());
        if (asyncRecord.isEmpty()) {
            return;
        }
        ApiInvocationRecordEntity record = asyncRecord.get();
        record.updateTerminalStatus(mapTaskStatus(taskStatus), outcome, now);
        repository.save(record);
        persistBatchItemsFromRecord(
                template,
                policy,
                record,
                environment,
                routeType,
                requestedReleaseVersion,
                resolvedReleaseVersion,
                request,
                batchResult,
                record.getInvocationExternalId(),
                batchResult.batchId(),
                task.getTaskExternalId(),
                auditId,
                now
        );
    }

    private void persistBatchItemsFromRecord(
            TemplateEntity template,
            ApiPolicyEntity policy,
            ApiInvocationRecordEntity parentRecord,
            String environment,
            String routeType,
            String requestedReleaseVersion,
            String resolvedReleaseVersion,
            BatchGenerateRequestBody request,
            BatchResultView batchResult,
            String parentInvocationExternalId,
            String batchExternalId,
            String taskExternalId,
            String auditId,
            Instant now
    ) {
        persistBatchItems(
                template,
                policy,
                parentRecord.getCredentialId(),
                parentRecord.getAccessAccount(),
                environment,
                routeType,
                requestedReleaseVersion,
                resolvedReleaseVersion,
                request,
                batchResult,
                parentInvocationExternalId,
                batchExternalId,
                taskExternalId,
                parentRecord.getIdempotencyRecordId(),
                auditId,
                now
        );
    }

    private void persistBatchItems(
            TemplateEntity template,
            ApiPolicyEntity policy,
            UUID credentialId,
            String accessAccount,
            String environment,
            String routeType,
            String requestedReleaseVersion,
            String resolvedReleaseVersion,
            BatchGenerateRequestBody request,
            BatchResultView batchResult,
            String parentInvocationExternalId,
            String batchExternalId,
            String taskExternalId,
            UUID idempotencyRecordId,
            String auditId,
            Instant now
    ) {
        for (BatchResultItemView item : batchResult.items()) {
            BatchGenerateRequestBody.BatchGenerateItemBody itemBody = request.items().stream()
                    .filter(candidate -> candidate.itemId().equals(item.itemId()))
                    .findFirst()
                    .orElseThrow();
            boolean hasArtifact = item.documentId() != null && !item.documentId().isBlank();
            boolean artifactSaved = policy.isSaveGeneratedDocuments() && hasArtifact;
            ApiInvocationRecordEntity itemRecord = new ApiInvocationRecordEntity(
                    UUID.randomUUID(),
                    newInvocationExternalId(),
                    InvocationKind.BATCH_ITEM,
                    mapItemStatus(item.status()),
                    environment,
                    template.getId(),
                    template.getExternalId(),
                    credentialId,
                    accessAccount,
                    request.requestId(),
                    request.idempotencyKey(),
                    routeType,
                    requestedReleaseVersion,
                    resolvedReleaseVersion,
                    item.output().format(),
                    item.output().mode(),
                    item.status(),
                    null,
                    parameterSanitizer.sanitizeBatchItem(itemBody, request, resolvedReleaseVersion),
                    item.documentId(),
                    null,
                    artifactSaved,
                    recordExpiresAt(policy, now),
                    documentExpiresAt(policy, artifactSaved, now),
                    batchExternalId,
                    parentInvocationExternalId,
                    item.itemId(),
                    taskExternalId,
                    idempotencyRecordId,
                    auditId,
                    true,
                    now,
                    now
            );
            repository.save(itemRecord);
        }
    }

    private Optional<ApiInvocationRecordEntity> findLiveRootRecord(
            UUID templateId,
            UUID credentialId,
            String idempotencyKey
    ) {
        return repository.findFirstByIdempotencyKeyAndTemplateIdAndCredentialIdAndInvocationKindInAndRecordExpiresAtAfterOrderByCreatedAtDesc(
                idempotencyKey,
                templateId,
                credentialId,
                ROOT_INVOCATION_KINDS,
                Instant.now()
        );
    }

    private UUID resolveIdempotencyRecordId(String idempotencyKey, UUID templateId) {
        return idempotencyService.findLiveRecord(idempotencyKey, templateId)
                .map(GenerationIdempotencyEntity::getId)
                .orElse(null);
    }

    private Instant recordExpiresAt(ApiPolicyEntity policy, Instant now) {
        return now.plus(policy.getInvocationRecordRetentionDays(), ChronoUnit.DAYS);
    }

    private Instant documentExpiresAt(ApiPolicyEntity policy, boolean artifactSaved, Instant now) {
        if (!artifactSaved) {
            return null;
        }
        return now.plus(policy.getDocumentRetentionDays(), ChronoUnit.DAYS);
    }

    private String newInvocationExternalId() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private InvocationStatus mapOutcomeToStatus(String outcome) {
        if (RuntimeGenerationAuditRecorder.OUTCOME_FAILURE.equals(outcome)) {
            return InvocationStatus.FAILED;
        }
        return InvocationStatus.SUCCEEDED;
    }

    private InvocationStatus mapItemStatus(String itemStatus) {
        if ("FAILED".equalsIgnoreCase(itemStatus)) {
            return InvocationStatus.FAILED;
        }
        return InvocationStatus.SUCCEEDED;
    }

    private InvocationStatus mapBatchRootStatus(BatchResultView batchResult) {
        boolean anyFailed = batchResult.items().stream()
                .anyMatch(item -> "FAILED".equalsIgnoreCase(item.status()));
        boolean anySucceeded = batchResult.items().stream()
                .anyMatch(item -> "SUCCEEDED".equalsIgnoreCase(item.status()));
        if (anyFailed && anySucceeded) {
            return InvocationStatus.PARTIAL_SUCCEEDED;
        }
        if (anyFailed) {
            return InvocationStatus.FAILED;
        }
        return InvocationStatus.SUCCEEDED;
    }

    private InvocationStatus mapTaskStatus(TaskStatus taskStatus) {
        return switch (taskStatus) {
            case ACCEPTED -> InvocationStatus.ACCEPTED;
            case PROCESSING -> InvocationStatus.PROCESSING;
            case SUCCEEDED -> InvocationStatus.SUCCEEDED;
            case FAILED -> InvocationStatus.FAILED;
            case PARTIAL_SUCCEEDED -> InvocationStatus.PARTIAL_SUCCEEDED;
            case EXPIRED -> InvocationStatus.EXPIRED;
            case CANCELLED -> InvocationStatus.CANCELLED;
        };
    }
}
