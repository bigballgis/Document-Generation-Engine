package com.bank.docgen.audit.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * LR-D1: hard-delete aged {@link ManagementAuditEventEntity} rows by {@code event_at}.
 */
@Repository
public interface ManagementAuditEventCleanupRepository
        extends JpaRepository<ManagementAuditEventEntity, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM ManagementAuditEventEntity e WHERE e.eventAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
