package com.bank.docgen.template.api;

/**
 * One nesting edge in a promotion-pack {@code clauseNestingGraph} (PP-C2 / ADR-0067).
 */
public record TemplateExportClauseNestingGraphEdgeView(
        String parentModuleCode,
        String childModuleCode,
        int depth
) {
}
