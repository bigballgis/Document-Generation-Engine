package com.bank.docgen.rendering.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatchTestRunRepository extends JpaRepository<BatchTestRunEntity, UUID> {

    List<BatchTestRunEntity> findByTemplateIdOrderByCreatedAtDesc(UUID templateId);

    List<BatchTestRunEntity> findByTemplateIdAndHiddenFalseOrderByCreatedAtDesc(UUID templateId);

    @Query("SELECT b FROM BatchTestRunEntity b WHERE b.templateId = :templateId "
            + "AND b.hidden = false AND b.invalidatedAt IS NULL "
            + "ORDER BY b.createdAt DESC")
    List<BatchTestRunEntity> findValidByTemplateIdOrderByCreatedAtDesc(@Param("templateId") UUID templateId);

    @Query("SELECT b FROM BatchTestRunEntity b WHERE b.templateId = :templateId "
            + "AND b.hidden = false AND b.invalidatedAt IS NULL "
            + "ORDER BY b.createdAt DESC")
    List<BatchTestRunEntity> findTopValidByTemplateId(@Param("templateId") UUID templateId, Pageable pageable);

    default Optional<BatchTestRunEntity> findLatestValidByTemplateId(UUID templateId) {
        List<BatchTestRunEntity> results = findTopValidByTemplateId(templateId, PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    long countByTemplateIdAndHiddenFalse(UUID templateId);
}
