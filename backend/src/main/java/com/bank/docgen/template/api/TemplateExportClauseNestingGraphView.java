package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Machine-readable clause nesting closure for promotion packs (PP-C2).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TemplateExportClauseNestingGraphView(
        List<TemplateExportClauseNestingGraphEdgeView> edges,
        Integer maxDepth
) {
    public TemplateExportClauseNestingGraphView {
        edges = DefensiveCopies.copyList(edges);
    }

    public static TemplateExportClauseNestingGraphView empty() {
        return new TemplateExportClauseNestingGraphView(List.of(), 0);
    }
}
