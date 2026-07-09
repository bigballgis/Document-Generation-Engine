package com.bank.docgen.template.persistence;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateVersionRepository extends JpaRepository<TemplateVersionEntity, UUID> {

    Optional<TemplateVersionEntity> findByTemplateIdAndDevVersionNumber(UUID templateId, int devVersionNumber);

    List<TemplateVersionEntity> findByTemplateIdOrderByDevVersionNumberDesc(UUID templateId);

    Optional<TemplateVersionEntity> findByTemplateIdAndReleaseVersion(UUID templateId, String releaseVersion);

    @Modifying
    @Query("""
            UPDATE TemplateVersionEntity v
            SET v.lifecycleStatus = :toStatus, v.updatedAt = :updatedAt
            WHERE v.templateId = :templateId AND v.lifecycleStatus = :fromStatus
            """)
    int bulkUpdateLifecycleStatus(
            @Param("templateId") UUID templateId,
            @Param("fromStatus") TemplateLifecycleStatus fromStatus,
            @Param("toStatus") TemplateLifecycleStatus toStatus,
            @Param("updatedAt") Instant updatedAt
    );

    @Modifying
    @Query("""
            UPDATE TemplateVersionEntity v
            SET v.lifecycleStatus = :toStatus, v.updatedAt = :updatedAt
            WHERE v.templateId = :templateId
            """)
    int bulkUpdateAllLifecycleStatus(
            @Param("templateId") UUID templateId,
            @Param("toStatus") TemplateLifecycleStatus toStatus,
            @Param("updatedAt") Instant updatedAt
    );
}
