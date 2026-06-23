package com.bank.docgen.rendering.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreviewRecordRepository extends JpaRepository<PreviewRecordEntity, UUID> {

    List<PreviewRecordEntity> findByTemplateIdOrderByCreatedAtDesc(UUID templateId);
}
