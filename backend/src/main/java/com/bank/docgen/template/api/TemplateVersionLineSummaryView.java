package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.domain.TemplateVersionLineKind;
import java.time.Instant;

public record TemplateVersionLineSummaryView(
        String devVersionId,
        int devVersionNumber,
        String releaseVersion,
        TemplateLifecycleStatus lifecycleStatus,
        ApprovalSubState approvalSubState,
        TemplateVersionLineKind lineKind,
        Instant updatedAt,
        String updatedBy,
        Boolean defaultRouteTarget,
        boolean cloneable,
        String updatedByDisplayName
) {
}
