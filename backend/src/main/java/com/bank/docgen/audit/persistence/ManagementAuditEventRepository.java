package com.bank.docgen.audit.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManagementAuditEventRepository extends JpaRepository<ManagementAuditEventEntity, UUID> {

    @Query("""
            SELECT e FROM ManagementAuditEventEntity e
            WHERE (:templateId IS NULL OR e.templateId = :templateId)
              AND (:eventType IS NULL OR e.eventType = :eventType)
              AND (:credentialId IS NULL OR e.credentialId = :credentialId)
              AND (:eventAtFrom IS NULL OR e.eventAt >= :eventAtFrom)
              AND (:eventAtTo IS NULL OR e.eventAt <= :eventAtTo)
              AND (:groupCode IS NULL OR e.groupCode = :groupCode)
            ORDER BY e.eventAt DESC
            """)
    List<ManagementAuditEventEntity> search(
            @Param("templateId") UUID templateId,
            @Param("eventType") String eventType,
            @Param("credentialId") UUID credentialId,
            @Param("eventAtFrom") Instant eventAtFrom,
            @Param("eventAtTo") Instant eventAtTo,
            @Param("groupCode") String groupCode
    );
}
