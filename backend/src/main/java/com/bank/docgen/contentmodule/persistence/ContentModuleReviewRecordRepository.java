package com.bank.docgen.contentmodule.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentModuleReviewRecordRepository
        extends JpaRepository<ContentModuleReviewRecordEntity, UUID> {

    List<ContentModuleReviewRecordEntity> findByModuleIdOrderByCreatedAtAsc(UUID moduleId);
}
