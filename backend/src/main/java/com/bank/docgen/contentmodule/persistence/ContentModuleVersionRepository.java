package com.bank.docgen.contentmodule.persistence;

import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContentModuleVersionRepository extends JpaRepository<ContentModuleVersionEntity, UUID> {

    Optional<ContentModuleVersionEntity> findByModuleIdAndSemanticVersion(UUID moduleId, String semanticVersion);

    List<ContentModuleVersionEntity> findByModuleIdOrderBySemanticVersionDesc(UUID moduleId);

    List<ContentModuleVersionEntity> findByModuleIdAndReviewStateAndLifecycleState(
            UUID moduleId,
            ContentModuleReviewState reviewState,
            ContentModuleLifecycleState lifecycleState
    );

    boolean existsByModuleIdAndSemanticVersion(UUID moduleId, String semanticVersion);

    List<ContentModuleVersionEntity> findByModuleIdAndReviewStateOrderBySemanticVersionDesc(
            UUID moduleId,
            ContentModuleReviewState reviewState
    );

    List<ContentModuleVersionEntity> findByModuleIdAndReviewStateAndLifecycleStateOrderBySemanticVersionDesc(
            UUID moduleId,
            ContentModuleReviewState reviewState,
            ContentModuleLifecycleState lifecycleState
    );

    List<ContentModuleVersionEntity> findByReviewStateOrderByUpdatedAtDesc(ContentModuleReviewState reviewState);

    @Query("""
            select v from ContentModuleVersionEntity v
            where v.reviewState = com.bank.docgen.contentmodule.domain.ContentModuleReviewState.DRAFT
              and v.rejectionReason is not null
              and trim(v.rejectionReason) <> ''
            order by v.updatedAt desc
            """)
    List<ContentModuleVersionEntity> findDraftVersionsWithRejectionReason();
}
