package com.bank.docgen.template.persistence;

import com.bank.docgen.audit.persistence.AuditSearchPage;
import java.time.Instant;
import java.util.UUID;

public interface TemplateLifecycleRecordRepositoryCustom {

    AuditSearchPage<TemplateLifecycleRecordEntity> searchPaged(
            UUID templateId,
            String eventType,
            Instant eventAtFrom,
            Instant eventAtTo,
            int page,
            int size
    );
}
