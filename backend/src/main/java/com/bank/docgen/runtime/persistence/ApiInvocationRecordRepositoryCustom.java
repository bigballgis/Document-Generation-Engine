package com.bank.docgen.runtime.persistence;

import com.bank.docgen.audit.persistence.AuditSearchPage;
import com.bank.docgen.runtime.domain.InvocationKind;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public interface ApiInvocationRecordRepositoryCustom {

    AuditSearchPage<ApiInvocationRecordEntity> searchManagementInvocations(
            UUID templateId,
            Collection<InvocationKind> kinds,
            Instant retentionAfter,
            String outcome,
            InvocationKind invocationKind,
            String requestId,
            Instant createdAfter,
            Instant createdBefore,
            UUID credentialId,
            int page,
            int size
    );
}
