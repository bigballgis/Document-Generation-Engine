package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
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
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvocationRecordService {

    private final ApiInvocationRecordRepository repository;
    private final InvocationParameterSanitizer parameterSanitizer;
    private final IdempotencyService idempotencyService;
    private final InvocationRecordMetadataSupport metadata;
    private final InvocationBatchItemPersistenceSupport batchItems;

    public InvocationRecordService(
            ApiInvocationRecordRepository repository,
            InvocationParameterSanitizer parameterSanitizer,
            IdempotencyService idempotencyService
    ) {
        this.repository = repository;
        this.parameterSanitizer = parameterSanitizer;
        this.idempotencyService = idempotencyService;
        this.metadata = new InvocationRecordMetadataSupport(repository, idempotencyService);
        this.batchItems = new InvocationBatchItemPersistenceSupport(repository, parameterSanitizer, metadata);
    }

    @Transactional(readOnly = true)
    public Optional<String> findExistingInvocationId(
            UUID templateId,
            UUID credentialId,
            String idempotencyKey
    ) {
        return metadata.findLiveRootRecord(templateId, credentialId, idempotencyKey)
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
        UUID idempotencyRecordId = metadata.resolveIdempotencyRecordId(request.idempotencyKey(), template.getId());
        String resolvedArtifactStorageKey = artifactStorageKey;
        if (resolvedArtifactStorageKey == null && hasArtifact) {
            resolvedArtifactStorageKey = idempotencyService.findLiveRecord(request.idempotencyKey(), template.getId())
                    .map(GenerationIdempotencyEntity::getResponseStorageKey)
                    .orElse(null);
        }
        boolean artifactSaved = policy.isSaveGeneratedDocuments() && hasArtifact;
        ApiInvocationRecordEntity entity = new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                metadata.newInvocationExternalId(),
                InvocationKind.SINGLE,
                InvocationStatusMappingSupport.mapOutcomeToStatus(outcome),
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
                metadata.recordExpiresAt(policy, now),
                metadata.documentExpiresAt(policy, artifactSaved, now),
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
        UUID idempotencyRecordId = metadata.resolveIdempotencyRecordId(request.idempotencyKey(), template.getId());
        String rootInvocationId = metadata.newInvocationExternalId();
        InvocationStatus rootStatus = InvocationStatusMappingSupport.mapBatchRootStatus(batchResult);
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
                metadata.recordExpiresAt(policy, now),
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
        batchItems.persistBatchItems(
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
        UUID idempotencyRecordId = metadata.resolveIdempotencyRecordId(request.idempotencyKey(), template.getId());
        ApiInvocationRecordEntity entity = new ApiInvocationRecordEntity(
                UUID.randomUUID(),
                metadata.newInvocationExternalId(),
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
                metadata.recordExpiresAt(policy, now),
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
        record.updateTerminalStatus(InvocationStatusMappingSupport.mapTaskStatus(taskStatus), outcome, now);
        repository.save(record);
        batchItems.persistBatchItemsFromRecord(
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
}
