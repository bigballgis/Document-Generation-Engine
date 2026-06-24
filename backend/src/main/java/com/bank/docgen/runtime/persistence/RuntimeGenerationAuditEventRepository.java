package com.bank.docgen.runtime.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuntimeGenerationAuditEventRepository extends JpaRepository<RuntimeGenerationAuditEventEntity, UUID> {
}
