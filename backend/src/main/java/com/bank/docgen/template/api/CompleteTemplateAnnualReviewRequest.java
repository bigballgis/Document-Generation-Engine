package com.bank.docgen.template.api;

import java.time.LocalDate;

/**
 * CE-G05 complete-annual-review body. Omit {@code nextReviewDue} to roll to
 * completion-day UTC date + 365 days.
 */
public record CompleteTemplateAnnualReviewRequest(
        LocalDate nextReviewDue
) {
}
