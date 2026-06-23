package com.bank.docgen.runtime.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationIdempotencyRepository extends JpaRepository<GenerationIdempotencyEntity, UUID> {

    Optional<GenerationIdempotencyEntity> findByIdempotencyKeyAndTemplateId(String idempotencyKey, UUID templateId);

    Optional<GenerationIdempotencyEntity> findByDocumentId(String documentId);
}
