package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.runtime.api.BatchGenerateRequestBody;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.domain.InvocationStatus;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.time.Instant;
import java.util.UUID;

/**
 * Package-private root invocation entity construction helpers.
 */
final class InvocationRecordEntitySupport {

    private final InvocationParameterSanitizer parameterSanitizer;
    private final IdempotencyService idempotencyService;
    private final InvocationRecordMetadataSupport metadata;
    private final ReleaseBundleFingerprintSupport fingerprintSupport;

    InvocationRecordEntitySupport(
            InvocationParameterSanitizer parameterSanitizer,
            IdempotencyService idempotencyService,
            InvocationRecordMetadataSupport metadata,
            ReleaseBundleFingerprintSupport fingerprintSupport
    ) {
        this.parameterSanitizer = parameterSanitizer;
        this.idempotencyService = idempotencyService;
        this.metadata = metadata;
        this.fingerprintSupport = fingerprintSupport;
    }

    ApiInvocationRecordEntity buildSingleSync(
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
            String auditId,
            Instant now
    ) {
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
                parameterSanitizer.sanitizeSingleRequest(request, resolvedReleaseVersion, template.getId()),
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
        applyFingerprint(entity, template.getId(), resolvedReleaseVersion);
        return entity;
    }

    ApiInvocationRecordEntity buildBatchRoot(
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
            String auditId,
            String rootInvocationId,
            UUID idempotencyRecordId,
            Instant now
    ) {
        InvocationStatus rootStatus = InvocationStatusMappingSupport.mapBatchRootStatus(batchResult);
        // G06-C4: BATCH_ROOT does not require fingerprint (no single assembly unit).
        return new ApiInvocationRecordEntity(
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
                parameterSanitizer.sanitizeBatchRequest(request, resolvedReleaseVersion, template.getId()),
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
    }

    ApiInvocationRecordEntity buildAsyncAccepted(
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
            String auditId,
            UUID idempotencyRecordId,
            Instant now
    ) {
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
                parameterSanitizer.sanitizeBatchRequest(request, resolvedReleaseVersion, template.getId()),
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
        applyFingerprint(entity, template.getId(), resolvedReleaseVersion);
        return entity;
    }

    private void applyFingerprint(
            ApiInvocationRecordEntity entity,
            UUID templateId,
            String resolvedReleaseVersion
    ) {
        fingerprintSupport.resolve(templateId, resolvedReleaseVersion).ifPresent(fingerprint ->
                entity.applyReleaseBundleFingerprint(fingerprint.snapshotId(), fingerprint.bundleHash())
        );
    }
}
