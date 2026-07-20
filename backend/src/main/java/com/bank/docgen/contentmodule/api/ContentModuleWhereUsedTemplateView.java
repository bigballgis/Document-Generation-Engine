package com.bank.docgen.contentmodule.api;

import com.bank.docgen.contentmodule.domain.ContentModuleWhereUsedReferenceKind;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;

/**
 * CE-G05 / IBL-E6 where-used template row (direct pin or nested closure).
 */
public record ContentModuleWhereUsedTemplateView(
        String id,
        String externalId,
        String name,
        String groupCode,
        TemplateLifecycleStatus lifecycleStatus,
        String pinnedSemanticVersion,
        ContentModuleWhereUsedReferenceKind referenceKind,
        int nestingDepth,
        String nestingPathSummary
) {
}
