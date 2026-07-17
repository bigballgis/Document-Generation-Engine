package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import java.time.Instant;
import java.time.LocalDate;

public record AnnualReviewDueAuthorTaskView(
        String templateId,
        String externalId,
        String groupCode,
        String name,
        LocalDate nextReviewDue,
        TemplateLifecycleStatus lifecycleStatus,
        Instant updatedAt
) {
}
