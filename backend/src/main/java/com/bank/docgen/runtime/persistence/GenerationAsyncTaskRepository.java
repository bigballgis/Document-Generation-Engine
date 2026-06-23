package com.bank.docgen.runtime.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenerationAsyncTaskRepository extends JpaRepository<GenerationAsyncTaskEntity, UUID> {

    Optional<GenerationAsyncTaskEntity> findByTaskExternalIdAndTemplateId(String taskExternalId, UUID templateId);

    Optional<GenerationAsyncTaskEntity> findByIdempotencyKeyAndTemplateId(String idempotencyKey, UUID templateId);
}
