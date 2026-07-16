package com.bank.docgen.legalhold.api;

import com.bank.docgen.legalhold.domain.LegalHoldScopeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateLegalHoldRequest(
        LegalHoldScopeType scopeType,
        String reason,
        UUID templateId,
        String templateExternalId,
        Instant effectiveFrom,
        Instant effectiveTo,
        List<String> invocationExternalIds
) {
}
