package com.bank.docgen.legalhold.api;

import com.bank.docgen.legalhold.domain.LegalHoldScopeType;
import com.bank.docgen.legalhold.domain.LegalHoldStatus;
import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LegalHoldView(
        UUID id,
        String holdExternalId,
        LegalHoldScopeType scopeType,
        LegalHoldStatus status,
        String reason,
        UUID templateId,
        String templateExternalId,
        Instant effectiveFrom,
        Instant effectiveTo,
        List<String> invocationExternalIds,
        int invocationCount,
        Instant createdAt,
        String createdByUsername,
        Instant releasedAt,
        String releasedByUsername
) {
    public LegalHoldView {
        invocationExternalIds = DefensiveCopies.copyStringList(invocationExternalIds);
    }
}
