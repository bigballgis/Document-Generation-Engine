package com.bank.docgen.runtime.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * LR-D1: hard-delete aged {@link RuntimeGenerationAuditEventEntity} rows by {@code event_at}.
 * CE-G04: load candidates then filter legal-hold exemptions before delete.
 */
@Repository
public interface RuntimeGenerationAuditEventCleanupRepository
        extends JpaRepository<RuntimeGenerationAuditEventEntity, UUID> {

    List<RuntimeGenerationAuditEventEntity> findByEventAtBefore(Instant cutoff);
}
