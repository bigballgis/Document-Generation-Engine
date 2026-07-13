package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultItemView;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.time.Instant;
import java.util.UUID;

/**
 * Package-private BATCH_ITEM persistence for sync and async completion paths.
 */
final class InvocationBatchItemPersistenceSupport {

    private final ApiInvocationRecordRepository repository;
    private final InvocationParameterSanitizer parameterSanitizer;
    private final InvocationRecordMetadataSupport metadata;

    InvocationBatchItemPersistenceSupport(
            ApiInvocationRecordRepository repository,
            InvocationParameterSanitizer parameterSanitizer,
            InvocationRecordMetadataSupport metadata
    ) {
        this.repository = repository;
        this.parameterSanitizer = parameterSanitizer;
        this.metadata = metadata;
    }

    void persistBatchItemsFromRecord(
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

    void persistBatchItems(
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
                    metadata.newInvocationExternalId(),
                    InvocationKind.BATCH_ITEM,
                    InvocationStatusMappingSupport.mapItemStatus(item.status()),
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
                    metadata.recordExpiresAt(policy, now),
                    metadata.documentExpiresAt(policy, artifactSaved, now),
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
}
