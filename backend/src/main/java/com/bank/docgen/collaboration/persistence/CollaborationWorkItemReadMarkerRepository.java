package com.bank.docgen.collaboration.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollaborationWorkItemReadMarkerRepository
        extends JpaRepository<CollaborationWorkItemReadMarkerEntity, UUID> {

    boolean existsByUserIdAndWorkItemId(String userId, UUID workItemId);

    @Query("""
            SELECT marker.workItemId FROM CollaborationWorkItemReadMarkerEntity marker
            WHERE marker.userId = :userId
              AND marker.workItemId IN :workItemIds
            """)
    List<UUID> findWorkItemIdListByUserIdAndWorkItemIdIn(
            @Param("userId") String userId,
            @Param("workItemIds") Collection<UUID> workItemIds
    );

    default Set<UUID> findWorkItemIdsByUserIdAndWorkItemIdIn(
            String userId,
            Collection<UUID> workItemIds
    ) {
        if (workItemIds == null || workItemIds.isEmpty()) {
            return Set.of();
        }
        return findWorkItemIdListByUserIdAndWorkItemIdIn(userId, workItemIds).stream()
                .collect(Collectors.toUnmodifiableSet());
    }
}
