package com.bank.docgen.audit.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagementAuditEventRepository
        extends JpaRepository<ManagementAuditEventEntity, UUID>, ManagementAuditEventRepositoryCustom {
}
