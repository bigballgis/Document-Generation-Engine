package com.bank.docgen.audit.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ManagementAuditEventRepositoryCustom {

    List<ManagementAuditEventEntity> search(
            UUID templateId,
            String eventType,
            UUID credentialId,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupCode
    );
}
