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

    /**
     * Resolves a release version row. When historical re-publish left duplicate
     * {@code release_version} values, prefer the highest {@code dev_version_number}
     * so runtime generate does not fail with NonUniqueResultException.
     */
    Optional<TemplateVersionEntity> findFirstByTemplateIdAndReleaseVersionOrderByDevVersionNumberDesc(
            UUID templateId, String releaseVersion);

    default Optional<TemplateVersionEntity> findByTemplateIdAndReleaseVersion(UUID templateId, String releaseVersion) {
        return findFirstByTemplateIdAndReleaseVersionOrderByDevVersionNumberDesc(templateId, releaseVersion);
    }

    List<TemplateVersionEntity> findByMasterRevisionIdAndDeletedAtIsNull(UUID masterRevisionId);

    List<TemplateVersionEntity> findByLifecycleStatusAndMasterRevisionIdNotNullAndDeletedAtIsNull(
            TemplateLifecycleStatus lifecycleStatus);

    List<TemplateVersionEntity> findByLifecycleStatusAndMasterRevisionIdIsNullAndDeletedAtIsNull(
            TemplateLifecycleStatus lifecycleStatus);

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
