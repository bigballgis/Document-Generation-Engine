package com.bank.docgen.template.service;

import com.bank.docgen.template.persistence.TemplateEntity;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

/**
 * CE-G05 — seed / roll {@code nextReviewDue} on template rows.
 */
@Component
public class TemplateAnnualReviewSupport {

    private static final int REVIEW_CYCLE_DAYS = 365;

    private final Clock clock;

    public TemplateAnnualReviewSupport(Clock clock) {
        this.clock = clock;
    }

    public LocalDate todayUtc() {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    /**
     * When a template first enters {@code PUBLISHED} with an empty due date, seed
     * publish-day UTC + 365. Does not overwrite an existing value.
     */
    public void seedOnEnterPublishedIfAbsent(TemplateEntity template) {
        if (template.getNextReviewDue() != null) {
            return;
        }
        template.setNextReviewDue(todayUtc().plusDays(REVIEW_CYCLE_DAYS));
    }

    public LocalDate defaultNextReviewDueAfterComplete() {
        return todayUtc().plusDays(REVIEW_CYCLE_DAYS);
    }
}
