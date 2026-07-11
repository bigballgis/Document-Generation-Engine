package com.bank.docgen.collaboration.persistence;

import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemStatus;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
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
        List<CollaborationWorkItemEntity> items = findAllOpenByTemplateIdAndQueue(templateId, queue);
        return items.isEmpty() ? Optional.empty() : Optional.of(items.getFirst());
    }

    default List<CollaborationWorkItemEntity> findAllOpenByTemplateIdAndQueue(
            UUID templateId,
            CollaborationWorkItemQueue queue
    ) {
        return findAllByTemplateIdAndQueueAndStatusInternal(
                templateId,
                queue,
                CollaborationWorkItemStatus.OPEN
        );
    }

    @Query("""
            SELECT item FROM CollaborationWorkItemEntity item
            WHERE item.status = :status
              AND item.deletedAt IS NULL
              AND item.queue IN :queues
              AND (
                    :wildcardScope = TRUE
                    OR item.groupCode IN :groupCodes
                  )
            ORDER BY item.createdAt DESC
            """)
    List<CollaborationWorkItemEntity> findOpenByQueuesAndGroupsNewestFirstInternal(
            @Param("status") CollaborationWorkItemStatus status,
            @Param("queues") Collection<CollaborationWorkItemQueue> queues,
            @Param("wildcardScope") boolean wildcardScope,
            @Param("groupCodes") Collection<String> groupCodes,
            Pageable pageable
    );

    default List<CollaborationWorkItemEntity> findOpenByQueuesAndGroupsNewestFirst(
            Collection<CollaborationWorkItemQueue> queues,
            Collection<String> groupCodes,
            Pageable pageable
    ) {
        boolean wildcardScope = groupCodes.size() == 1 && groupCodes.contains("*");
        return findOpenByQueuesAndGroupsNewestFirstInternal(
                CollaborationWorkItemStatus.OPEN,
                queues,
                wildcardScope,
                groupCodes,
                pageable
        );
    }

    @Query("""
            SELECT COUNT(item) FROM CollaborationWorkItemEntity item
            WHERE item.status = :status
              AND item.deletedAt IS NULL
              AND item.queue IN :queues
              AND (
                    :wildcardScope = TRUE
                    OR item.groupCode IN :groupCodes
                  )
              AND NOT EXISTS (
                    SELECT marker FROM CollaborationWorkItemReadMarkerEntity marker
                    WHERE marker.workItemId = item.id
                      AND marker.userId = :userId
                  )
            """)
    long countOpenUnreadByQueuesAndGroupsInternal(
            @Param("status") CollaborationWorkItemStatus status,
            @Param("queues") Collection<CollaborationWorkItemQueue> queues,
            @Param("wildcardScope") boolean wildcardScope,
            @Param("groupCodes") Collection<String> groupCodes,
            @Param("userId") String userId
    );

    default long countOpenUnreadByQueuesAndGroups(
            Collection<CollaborationWorkItemQueue> queues,
            Collection<String> groupCodes,
            String userId
    ) {
        boolean wildcardScope = groupCodes.size() == 1 && groupCodes.contains("*");
        return countOpenUnreadByQueuesAndGroupsInternal(
                CollaborationWorkItemStatus.OPEN,
                queues,
                wildcardScope,
                groupCodes,
                userId
        );
    }

    @Query("""
            SELECT item.id FROM CollaborationWorkItemEntity item
            WHERE item.status = :status
              AND item.deletedAt IS NULL
              AND item.queue IN :queues
              AND (
                    :wildcardScope = TRUE
                    OR item.groupCode IN :groupCodes
                  )
              AND NOT EXISTS (
                    SELECT marker FROM CollaborationWorkItemReadMarkerEntity marker
                    WHERE marker.workItemId = item.id
                      AND marker.userId = :userId
                  )
            """)
    List<UUID> findOpenUnreadIdsByQueuesAndGroupsInternal(
            @Param("status") CollaborationWorkItemStatus status,
            @Param("queues") Collection<CollaborationWorkItemQueue> queues,
            @Param("wildcardScope") boolean wildcardScope,
            @Param("groupCodes") Collection<String> groupCodes,
            @Param("userId") String userId
    );

    default List<UUID> findOpenUnreadIdsByQueuesAndGroups(
            Collection<CollaborationWorkItemQueue> queues,
            Collection<String> groupCodes,
            String userId
    ) {
        boolean wildcardScope = groupCodes.size() == 1 && groupCodes.contains("*");
        return findOpenUnreadIdsByQueuesAndGroupsInternal(
                CollaborationWorkItemStatus.OPEN,
                queues,
                wildcardScope,
                groupCodes,
                userId
        );
    }

    @Query("""
            SELECT item FROM CollaborationWorkItemEntity item
            WHERE item.id = :workItemId
              AND item.status = :status
              AND item.deletedAt IS NULL
              AND item.queue IN :queues
              AND (
                    :wildcardScope = TRUE
                    OR item.groupCode IN :groupCodes
                  )
            """)
    Optional<CollaborationWorkItemEntity> findVisibleOpenByIdInternal(
            @Param("workItemId") UUID workItemId,
            @Param("status") CollaborationWorkItemStatus status,
            @Param("queues") Collection<CollaborationWorkItemQueue> queues,
            @Param("wildcardScope") boolean wildcardScope,
            @Param("groupCodes") Collection<String> groupCodes
    );

    default Optional<CollaborationWorkItemEntity> findVisibleOpenById(
            UUID workItemId,
            Collection<CollaborationWorkItemQueue> queues,
            Collection<String> groupCodes
    ) {
        boolean wildcardScope = groupCodes.size() == 1 && groupCodes.contains("*");
        return findVisibleOpenByIdInternal(
                workItemId,
                CollaborationWorkItemStatus.OPEN,
                queues,
                wildcardScope,
                groupCodes
        );
    }
}
