package com.bank.docgen.audit.service;

import static com.bank.docgen.audit.service.ManagementAuditEventTypes.TEMPLATE_ANNUAL_REVIEW_COMPLETED;

import com.bank.docgen.template.persistence.TemplateEntity;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * CE-G05 — annual-review complete audit (no variables / credentials / clause body).
 */
@Component
class TemplateAnnualReviewAuditRecorder {

    private final ManagementAuditEventWriter eventWriter;

    TemplateAnnualReviewAuditRecorder(ManagementAuditEventWriter eventWriter) {
        this.eventWriter = eventWriter;
    }

    @Transactional
    void recordCompleted(
            TemplateEntity template,
            LocalDate previousNextReviewDue,
            LocalDate newNextReviewDue,
            String actorUsername,
            String actorSummary
    ) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("templateId", template.getId().toString());
        detail.put("externalId", template.getExternalId());
        detail.put("groupCode", template.getGroupCode());
        if (previousNextReviewDue != null) {
            detail.put("previousNextReviewDue", previousNextReviewDue.toString());
        }
        detail.put("newNextReviewDue", newNextReviewDue.toString());
        detail.put("actorUsername", actorUsername);
        eventWriter.persist(
                TEMPLATE_ANNUAL_REVIEW_COMPLETED,
                template.getId(),
                template.getGroupCode(),
                null,
                null,
                null,
                eventWriter.writeJsonMap(detail),
                false,
                null,
                actorUsername,
                actorSummary,
                null,
                eventWriter.truncate(
                        TEMPLATE_ANNUAL_REVIEW_COMPLETED + " " + template.getExternalId()
                ),
                eventWriter.writeJson(List.of())
        );
    }
}
