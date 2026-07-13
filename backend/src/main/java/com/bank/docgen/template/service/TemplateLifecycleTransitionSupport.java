package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.time.Instant;
import java.util.UUID;

/**
 * Package-private helpers for lifecycle status transition, audit rows, and version bulk sync.
 * Does not own state-machine eligibility rules — callers remain responsible for gates.
 */
final class TemplateLifecycleTransitionSupport {

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateLifecycleRecordRepository lifecycleRecordRepository;

    TemplateLifecycleTransitionSupport(
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            TemplateLifecycleRecordRepository lifecycleRecordRepository
    ) {
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.lifecycleRecordRepository = lifecycleRecordRepository;
    }

    void transition(
            TemplateEntity template,
            TemplateLifecycleStatus toStatus,
            LifecycleAction action,
            LifecycleDecision decision,
            String comment,
            ManagementSessionClaims session
    ) {
        TemplateLifecycleStatus from = template.getLifecycleStatus();
        template.setLifecycleStatus(toStatus);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);
        recordLifecycle(template, action, from, toStatus, decision, comment, null, session);
    }

    void recordLifecycle(
            TemplateEntity template,
            LifecycleAction action,
            TemplateLifecycleStatus from,
            TemplateLifecycleStatus to,
            LifecycleDecision decision,
            String comment,
            String releaseVersion,
            ManagementSessionClaims session
    ) {
        lifecycleRecordRepository.save(new TemplateLifecycleRecordEntity(
                UUID.randomUUID(),
                template.getId(),
                action,
                from,
                to,
                decision,
                comment,
                releaseVersion,
                session.username()
        ));
    }

    void syncPublishedVersionsToStopped(UUID templateId) {
        templateVersionRepository.bulkUpdateLifecycleStatus(
                templateId,
                TemplateLifecycleStatus.PUBLISHED,
                TemplateLifecycleStatus.STOPPED,
                Instant.now()
        );
    }

    void syncStoppedVersionsToPublished(UUID templateId) {
        templateVersionRepository.bulkUpdateLifecycleStatus(
                templateId,
                TemplateLifecycleStatus.STOPPED,
                TemplateLifecycleStatus.PUBLISHED,
                Instant.now()
        );
    }

    void syncAllVersionsToDeprecated(UUID templateId) {
        templateVersionRepository.bulkUpdateAllLifecycleStatus(
                templateId,
                TemplateLifecycleStatus.DEPRECATED,
                Instant.now()
        );
    }

    boolean hasCallableVersions(UUID templateId) {
        return templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId).stream()
                .anyMatch(version -> version.getLifecycleStatus() == TemplateLifecycleStatus.PUBLISHED
                        && version.getReleaseVersion() != null
                        && !version.getReleaseVersion().isBlank());
    }
}
