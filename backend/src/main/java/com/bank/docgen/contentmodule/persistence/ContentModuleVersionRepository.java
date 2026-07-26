package com.bank.docgen.contentmodule.persistence;

import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentModuleVersionRepository extends JpaRepository<ContentModuleVersionEntity, UUID> {

    Optional<ContentModuleVersionEntity> findByModuleIdAndSemanticVersion(UUID moduleId, String semanticVersion);

    @Query("""
            select v from ContentModuleVersionEntity v
            where v.moduleId = :moduleId
            order by v.versionMajor desc, v.versionMinor desc, v.versionPatch desc, v.semanticVersion desc
            """)
    List<ContentModuleVersionEntity> findByModuleIdOrderBySemanticVersionDesc(@Param("moduleId") UUID moduleId);

    List<ContentModuleVersionEntity> findByModuleIdIn(Collection<UUID> moduleIds);

    List<ContentModuleVersionEntity> findByModuleIdAndReviewStateAndLifecycleState(
            UUID moduleId,
            ContentModuleReviewState reviewState,
            ContentModuleLifecycleState lifecycleState
    );

    boolean existsByModuleIdAndSemanticVersion(UUID moduleId, String semanticVersion);

    @Query("""
            select v from ContentModuleVersionEntity v
            where v.moduleId = :moduleId
              and v.reviewState = :reviewState
            order by v.versionMajor desc, v.versionMinor desc, v.versionPatch desc, v.semanticVersion desc
            """)
    List<ContentModuleVersionEntity> findByModuleIdAndReviewStateOrderBySemanticVersionDesc(
            @Param("moduleId") UUID moduleId,
            @Param("reviewState") ContentModuleReviewState reviewState
    );

    @Query("""
            select v from ContentModuleVersionEntity v
            where v.moduleId = :moduleId
              and v.reviewState = :reviewState
              and v.lifecycleState = :lifecycleState
            order by v.versionMajor desc, v.versionMinor desc, v.versionPatch desc, v.semanticVersion desc
            """)
    List<ContentModuleVersionEntity> findByModuleIdAndReviewStateAndLifecycleStateOrderBySemanticVersionDesc(
            @Param("moduleId") UUID moduleId,
            @Param("reviewState") ContentModuleReviewState reviewState,
            @Param("lifecycleState") ContentModuleLifecycleState lifecycleState
    );

    List<ContentModuleVersionEntity> findByReviewStateOrderByUpdatedAtDesc(ContentModuleReviewState reviewState);

    /**
     * PRR-A03: bounded SUBMITTED scan page (Pageable / LIMIT).
     */
    List<ContentModuleVersionEntity> findByReviewStateOrderByUpdatedAtDesc(
            ContentModuleReviewState reviewState,
            Pageable pageable
    );

    @Query("""
            select v from ContentModuleVersionEntity v
            where v.reviewState = com.bank.docgen.contentmodule.domain.ContentModuleReviewState.DRAFT
              and v.rejectionReason is not null
              and trim(v.rejectionReason) <> ''
            order by v.updatedAt desc
            """)
    List<ContentModuleVersionEntity> findDraftVersionsWithRejectionReason();
}
