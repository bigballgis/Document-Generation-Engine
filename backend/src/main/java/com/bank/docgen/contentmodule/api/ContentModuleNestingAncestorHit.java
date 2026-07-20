package com.bank.docgen.contentmodule.api;

import java.util.UUID;

/**
 * Ancestor CM version that nests the queried module (for deep where-used).
 */
public record ContentModuleNestingAncestorHit(
        UUID ancestorVersionId,
        UUID ancestorModuleId,
        String ancestorModuleCode,
        int nestingDepth,
        String nestingPathSummary
) {
}
