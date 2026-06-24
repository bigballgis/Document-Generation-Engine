package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import java.time.Instant;

public record TemplateReleaseVersionView(
        String releaseVersion,
        int devVersionNumber,
        TemplateLifecycleStatus lifecycleStatus,
        Instant updatedAt,
        String updatedBy,
        boolean defaultRouteTarget
) {
}
