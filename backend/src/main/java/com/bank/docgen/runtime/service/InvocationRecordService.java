package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.domain.TaskStatus;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.runtime.persistence.GenerationAsyncTaskEntity;
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
    private final InvocationRecordMetadataSupport metadata;
    private final InvocationBatchItemPersistenceSupport batchItems;
    private final InvocationRecordEntitySupport entities;

    public InvocationRecordService(
            ApiInvocationRecordRepository repository,
            InvocationParameterSanitizer parameterSanitizer,
            IdempotencyService idempotencyService
    ) {
        this.repository = repository;
        this.metadata = new InvocationRecordMetadataSupport(repository, idempotencyService);
        this.batchItems = new InvocationBatchItemPersistenceSupport(repository, parameterSanitizer, metadata);
        this.entities = new InvocationRecordEntitySupport(parameterSanitizer, idempotencyService, metadata);
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
        ApiInvocationRecordEntity entity = entities.buildSingleSync(
                template,
                policy,
                session,
                environment,
                routeType,
                requestedReleaseVersion,
                resolvedReleaseVersion,
                request,
                documentId,
                artifactStorageKey,
                outcome,
                auditId,
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
        ApiInvocationRecordEntity root = entities.buildBatchRoot(
                template,
                policy,
                session,
                environment,
                routeType,
                requestedReleaseVersion,
                resolvedReleaseVersion,
                request,
                batchResult,
                outcome,
                auditId,
                rootInvocationId,
                idempotencyRecordId,
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
        ApiInvocationRecordEntity entity = entities.buildAsyncAccepted(
                template,
                policy,
                session,
                environment,
                routeType,
                requestedReleaseVersion,
                resolvedReleaseVersion,
                request,
                taskExternalId,
                batchExternalId,
                auditId,
                idempotencyRecordId,
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
