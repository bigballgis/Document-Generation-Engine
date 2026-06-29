package com.bank.docgen.collaboration.persistence;

import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemStatus;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollaborationWorkItemRepository extends JpaRepository<CollaborationWorkItemEntity, UUID> {

    @Query("""
            SELECT item FROM CollaborationWorkItemEntity item
            WHERE item.status = :status
              AND item.deletedAt IS NULL
              AND item.queue IN :queues
              AND (
                    :wildcardScope = TRUE
                    OR item.groupCode IN :groupCodes
                  )
            ORDER BY item.createdAt ASC
            """)
    List<CollaborationWorkItemEntity> findOpenByQueuesAndGroupsInternal(
            @Param("status") CollaborationWorkItemStatus status,
            @Param("queues") Collection<CollaborationWorkItemQueue> queues,
            @Param("wildcardScope") boolean wildcardScope,
            @Param("groupCodes") Collection<String> groupCodes
    );

    default List<CollaborationWorkItemEntity> findOpenByQueuesAndGroups(
            Collection<CollaborationWorkItemQueue> queues,
            Collection<String> groupCodes
    ) {
        boolean wildcardScope = groupCodes.size() == 1 && groupCodes.contains("*");
        return findOpenByQueuesAndGroupsInternal(
                CollaborationWorkItemStatus.OPEN,
                queues,
                wildcardScope,
                groupCodes
        );
    }

    @Query("""
            SELECT item FROM CollaborationWorkItemEntity item
            WHERE item.status = :status
              AND item.deletedAt IS NULL
              AND item.queue IN :queues
            ORDER BY item.createdAt ASC
            """)
    List<CollaborationWorkItemEntity> findOpenEscalationCandidatesInternal(
            @Param("status") CollaborationWorkItemStatus status,
            @Param("queues") Collection<CollaborationWorkItemQueue> queues
    );

    default List<CollaborationWorkItemEntity> findOpenEscalationCandidates() {
        return findOpenEscalationCandidatesInternal(
                CollaborationWorkItemStatus.OPEN,
                EnumSet.of(
                        CollaborationWorkItemQueue.TEST,
                        CollaborationWorkItemQueue.APPROVAL,
                        CollaborationWorkItemQueue.PENDING_RELEASE,
                        CollaborationWorkItemQueue.REMEDIATION
                )
        );
    }

    boolean existsByStatusAndQueueAndSourceWorkItemIdAndDeletedAtIsNull(
            CollaborationWorkItemStatus status,
            CollaborationWorkItemQueue queue,
            UUID sourceWorkItemId
    );

    default boolean existsOpenEscalationForSource(UUID sourceWorkItemId) {
        return existsByStatusAndQueueAndSourceWorkItemIdAndDeletedAtIsNull(
                CollaborationWorkItemStatus.OPEN,
                CollaborationWorkItemQueue.ESCALATION,
                sourceWorkItemId
        );
    }

    @Query("""
            SELECT item FROM CollaborationWorkItemEntity item
            WHERE item.templateId = :templateId
              AND item.queue = :queue
              AND item.status = :status
              AND item.deletedAt IS NULL
            ORDER BY item.createdAt ASC
            """)
    List<CollaborationWorkItemEntity> findAllByTemplateIdAndQueueAndStatusInternal(
            @Param("templateId") UUID templateId,
            @Param("queue") CollaborationWorkItemQueue queue,
            @Param("status") CollaborationWorkItemStatus status
    );

    default Optional<CollaborationWorkItemEntity> findOpenByTemplateIdAndQueue(
            UUID templateId,
            CollaborationWorkItemQueue queue
    ) {
        List<CollaborationWorkItemEntity> items = findAllByTemplateIdAndQueueAndStatusInternal(
                templateId,
                queue,
                CollaborationWorkItemStatus.OPEN
        );
        return items.isEmpty() ? Optional.empty() : Optional.of(items.getFirst());
    }
}
