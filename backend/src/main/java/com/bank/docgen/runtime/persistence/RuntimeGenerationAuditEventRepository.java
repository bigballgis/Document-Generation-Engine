package com.bank.docgen.runtime.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuntimeGenerationAuditEventRepository extends JpaRepository<RuntimeGenerationAuditEventEntity, UUID> {

    long countByTemplateIdInAndEventAtAfterAndEventTypeIn(
            Collection<UUID> templateIds,
            Instant eventAt,
            Collection<String> eventTypes
    );
}
