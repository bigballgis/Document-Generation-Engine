package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.CompositionInclusionDecision;

/**
 * Non-sensitive audit/invocation summary entry (ADR-0063).
 */
public record CompositionInclusionSummaryEntryView(
        String referenceKey,
        CompositionInclusionDecision decision,
        String matchedRuleId
) {
}
