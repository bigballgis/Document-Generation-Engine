package com.bank.docgen.rendering.persistence;

import com.bank.docgen.rendering.domain.PreviewStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PreviewRecordRepository extends JpaRepository<PreviewRecordEntity, UUID> {

    List<PreviewRecordEntity> findByTemplateIdOrderByCreatedAtDesc(UUID templateId);

    /**
     * PRR-A02: TopN preview history at DB (Pageable / LIMIT).
     */
    List<PreviewRecordEntity> findByTemplateIdOrderByCreatedAtDesc(UUID templateId, Pageable pageable);

    List<PreviewRecordEntity> findByTemplateIdAndTemplateVersionIdAndStatus(
            UUID templateId,
            UUID templateVersionId,
            PreviewStatus status
    );

    @Query("SELECT p FROM PreviewRecordEntity p WHERE p.expiresAt <= :now "
            + "AND p.tempArtifactCleaned = false "
            + "AND p.batchTestRunId IS NULL")
    List<PreviewRecordEntity> findExpiredTempPreviews(@Param("now") Instant now);
}
