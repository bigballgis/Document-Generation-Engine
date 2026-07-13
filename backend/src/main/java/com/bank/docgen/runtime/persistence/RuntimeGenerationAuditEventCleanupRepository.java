package com.bank.docgen.runtime.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * LR-D1: hard-delete aged {@link RuntimeGenerationAuditEventEntity} rows by {@code event_at}.
 */
@Repository
public interface RuntimeGenerationAuditEventCleanupRepository
        extends JpaRepository<RuntimeGenerationAuditEventEntity, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RuntimeGenerationAuditEventEntity e WHERE e.eventAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
