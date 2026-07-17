package com.bank.docgen.contentmodule.api;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;

public record ContentModuleWhereUsedTemplateView(
        String id,
        String externalId,
        String name,
        String groupCode,
        TemplateLifecycleStatus lifecycleStatus,
        String pinnedSemanticVersion
) {
}
