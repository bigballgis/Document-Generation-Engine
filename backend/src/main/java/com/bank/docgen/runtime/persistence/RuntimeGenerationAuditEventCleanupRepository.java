package com.bank.docgen.runtime.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * LR-D1 / PRR-A01: hard-delete aged {@link RuntimeGenerationAuditEventEntity} rows by {@code event_at}.
 * CE-G04: load candidates in bounded pages then filter legal-hold exemptions before delete.
 */
@Repository
public interface RuntimeGenerationAuditEventCleanupRepository
        extends JpaRepository<RuntimeGenerationAuditEventEntity, UUID> {

    /**
     * Bounded candidate load for retention cleanup (PRR-A01). Prefer this over any unbounded load.
     */
    List<RuntimeGenerationAuditEventEntity> findByEventAtBefore(Instant cutoff, Pageable pageable);
}
