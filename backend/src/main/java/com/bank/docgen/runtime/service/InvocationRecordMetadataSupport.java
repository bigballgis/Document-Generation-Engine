package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.runtime.domain.InvocationKind;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordRepository;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Package-private retention / idempotency / id helpers for invocation records.
 */
final class InvocationRecordMetadataSupport {

    static final List<InvocationKind> ROOT_INVOCATION_KINDS = List.of(
            InvocationKind.SINGLE,
            InvocationKind.BATCH_ROOT,
            InvocationKind.ASYNC_TASK
    );

    private final ApiInvocationRecordRepository repository;
    private final IdempotencyService idempotencyService;

    InvocationRecordMetadataSupport(
            ApiInvocationRecordRepository repository,
            IdempotencyService idempotencyService
    ) {
        this.repository = repository;
        this.idempotencyService = idempotencyService;
    }

    Optional<ApiInvocationRecordEntity> findLiveRootRecord(
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

    UUID resolveIdempotencyRecordId(String idempotencyKey, UUID templateId) {
        return idempotencyService.findLiveRecord(idempotencyKey, templateId)
                .map(GenerationIdempotencyEntity::getId)
                .orElse(null);
    }

    Instant recordExpiresAt(ApiPolicyEntity policy, Instant now) {
        return now.plus(policy.getInvocationRecordRetentionDays(), ChronoUnit.DAYS);
    }

    Instant documentExpiresAt(ApiPolicyEntity policy, boolean artifactSaved, Instant now) {
        if (!artifactSaved) {
            return null;
        }
        return now.plus(policy.getDocumentRetentionDays(), ChronoUnit.DAYS);
    }

    String newInvocationExternalId() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
