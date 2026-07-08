package com.bank.docgen.runtime.persistence;

import com.bank.docgen.audit.persistence.AuditSearchPage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface RuntimeGenerationAuditEventRepositoryCustom {

    List<RuntimeGenerationAuditEventEntity> search(
            UUID templateId,
            String eventType,
            UUID credentialId,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupCode,
            String requestId
    );

    AuditSearchPage<RuntimeGenerationAuditEventEntity> searchPaged(
            UUID templateId,
            String eventType,
            UUID credentialId,
            Instant eventAtFrom,
            Instant eventAtTo,
            String groupCode,
            String requestId,
            int page,
            int size
    );
}
