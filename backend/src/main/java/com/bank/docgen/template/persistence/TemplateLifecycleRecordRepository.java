package com.bank.docgen.template.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateLifecycleRecordRepository extends JpaRepository<TemplateLifecycleRecordEntity, UUID> {

    List<TemplateLifecycleRecordEntity> findByTemplateIdOrderByCreatedAtDesc(UUID templateId);

    List<TemplateLifecycleRecordEntity> findAllByOrderByCreatedAtDesc();
}
