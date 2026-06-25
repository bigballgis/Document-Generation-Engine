package com.bank.docgen.rendering.persistence;

import com.bank.docgen.rendering.domain.PreviewStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreviewRecordRepository extends JpaRepository<PreviewRecordEntity, UUID> {

    List<PreviewRecordEntity> findByTemplateIdOrderByCreatedAtDesc(UUID templateId);

    List<PreviewRecordEntity> findByTemplateIdAndTemplateVersionIdAndStatus(
            UUID templateId,
            UUID templateVersionId,
            PreviewStatus status
    );
}
