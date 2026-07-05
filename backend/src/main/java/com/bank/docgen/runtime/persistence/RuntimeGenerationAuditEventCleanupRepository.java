package com.bank.docgen.runtime.persistence;

import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventEntity;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * LR-D1: cleanup repository for {@link RuntimeGenerationAuditEventEntity}. Deletes records older
 * than the configured retention window. Mirrors the invocation-record cleanup pattern (ADR-0040).
 */
@Repository
public interface RuntimeGenerationAuditEventCleanupRepository
        extends JpaRepository<RuntimeGenerationAuditEventEntity, Long> {

    @Modifying
    @Query("DELETE FROM RuntimeGenerationAuditEventEntity e WHERE e.eventAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    default int deleteOlderThanDays(int days) {
        return deleteOlderThan(Instant.now().minusSeconds((long) days * 86400));
    }
}
