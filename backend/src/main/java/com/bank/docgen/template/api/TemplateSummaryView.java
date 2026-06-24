package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import java.time.Instant;

public record TemplateSummaryView(
        String id,
        String externalId,
        String groupCode,
        String name,
        TemplateLifecycleStatus lifecycleStatus,
        String releaseVersion,
        int releaseVersionCount,
        String masterId,
        String updatedBy,
        Instant updatedAt
) {
}
