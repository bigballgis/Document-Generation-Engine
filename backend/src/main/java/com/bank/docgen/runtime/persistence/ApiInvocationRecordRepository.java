package com.bank.docgen.runtime.persistence;

import com.bank.docgen.runtime.domain.InvocationKind;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApiInvocationRecordRepository
        extends JpaRepository<ApiInvocationRecordEntity, UUID>, ApiInvocationRecordRepositoryCustom {

    Optional<ApiInvocationRecordEntity> findByInvocationExternalId(String invocationExternalId);

    Optional<ApiInvocationRecordEntity> findByTaskExternalId(String taskExternalId);

    List<ApiInvocationRecordEntity> findByBatchExternalIdAndInvocationKind(
            String batchExternalId,
            InvocationKind invocationKind
    );

    List<ApiInvocationRecordEntity> findByBatchExternalIdAndInvocationKindAndCredentialId(
            String batchExternalId,
            InvocationKind invocationKind,
            UUID credentialId
    );

    long countByBatchExternalIdAndInvocationKindAndCredentialId(
            String batchExternalId,
            InvocationKind invocationKind,
            UUID credentialId
    );

    List<ApiInvocationRecordEntity> findByRecordExpiresAtBefore(Instant before);

    List<ApiInvocationRecordEntity> findByDocumentExpiresAtBeforeAndArtifactStorageKeyIsNotNull(Instant before);

    Page<ApiInvocationRecordEntity> findByTemplateIdAndCredentialIdAndInvocationKindInAndRecordExpiresAtAfterOrderByCreatedAtDesc(
            UUID templateId,
            UUID credentialId,
            Collection<InvocationKind> kinds,
            Instant now,
            Pageable pageable
    );

    Page<ApiInvocationRecordEntity> findByTemplateIdAndCredentialIdAndInvocationKindInAndRequestIdAndRecordExpiresAtAfterOrderByCreatedAtDesc(
            UUID templateId,
            UUID credentialId,
            Collection<InvocationKind> kinds,
            String requestId,
            Instant now,
            Pageable pageable
    );

    Optional<ApiInvocationRecordEntity> findFirstByIdempotencyKeyAndTemplateIdAndCredentialIdAndInvocationKindInAndRecordExpiresAtAfterOrderByCreatedAtDesc(
            String idempotencyKey,
            UUID templateId,
            UUID credentialId,
            Collection<InvocationKind> kinds,
            Instant now
    );

    Page<ApiInvocationRecordEntity> findByTemplateIdAndInvocationKindInAndRecordExpiresAtAfterOrderByCreatedAtDesc(
            UUID templateId,
            Collection<InvocationKind> kinds,
            Instant now,
            Pageable pageable
    );
}
