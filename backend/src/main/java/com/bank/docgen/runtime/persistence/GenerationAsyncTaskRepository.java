package com.bank.docgen.runtime.persistence;

import com.bank.docgen.runtime.domain.TaskStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GenerationAsyncTaskRepository extends JpaRepository<GenerationAsyncTaskEntity, UUID> {

    Optional<GenerationAsyncTaskEntity> findByTaskExternalIdAndTemplateId(String taskExternalId, UUID templateId);

    Optional<GenerationAsyncTaskEntity> findByIdempotencyKeyAndTemplateId(String idempotencyKey, UUID templateId);

    List<GenerationAsyncTaskEntity> findByStatusAndUpdatedAtBefore(TaskStatus status, Instant updatedBefore);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE GenerationAsyncTaskEntity task
            SET task.status = :newStatus,
                task.updatedAt = :now
            WHERE task.id = :id
              AND task.status = :expectedStatus
              AND task.updatedAt = :expectedUpdatedAt
            """)
    int compareAndSetStatus(
            @Param("id") UUID id,
            @Param("newStatus") TaskStatus newStatus,
            @Param("now") Instant now,
            @Param("expectedStatus") TaskStatus expectedStatus,
            @Param("expectedUpdatedAt") Instant expectedUpdatedAt
    );
}
