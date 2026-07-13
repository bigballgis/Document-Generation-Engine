package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.TemplateDevVersionCreatedView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BiFunction;
import org.springframework.http.HttpStatus;

/**
 * Package-private clone / abandon mutation bodies for TemplateVersionLineService.
 */
final class TemplateVersionLineMutationSupport {

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateCurrentVersionResolver templateCurrentVersionResolver;
    private final TemplateVersionLineCloneSupport cloneSupport;

    TemplateVersionLineMutationSupport(
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            TemplateCurrentVersionResolver templateCurrentVersionResolver,
            TemplateVersionLineCloneSupport cloneSupport
    ) {
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.templateCurrentVersionResolver = templateCurrentVersionResolver;
        this.cloneSupport = cloneSupport;
    }

    TemplateDevVersionCreatedView cloneReleaseVersion(
            TemplateEntity template,
            String releaseVersion,
            ManagementSessionClaims session
    ) {
        UUID templateId = template.getId();
        if (templateCurrentVersionResolver.hasInFlightDevVersion(templateId)) {
            throw new TemplateGovernanceException(
                    ApiErrorCodes.TEMPLATE_DEV_LINE_IN_FLIGHT,
                    "api.error.template.devLineInFlight",
                    HttpStatus.CONFLICT
            );
        }
        TemplateVersionEntity source = templateVersionRepository
                .findByTemplateIdAndReleaseVersion(templateId, releaseVersion)
                .orElseThrow(TemplateNotFoundException::new);

        if (source.getReleaseVersion() == null || source.getReleaseVersion().isBlank()) {
            throw new TemplateNotFoundException();
        }
        TemplateVersionEntity target = new TemplateVersionEntity(UUID.randomUUID(), templateId, session.username());
        target.setDevVersionNumber(templateCurrentVersionResolver.maxDevVersionNumber(templateId) + 1);
        target.setMasterCatalogVersion(source.getMasterCatalogVersion());
        target.setRulesJson(source.getRulesJson());
        target.setRenderProfileVersion(source.getRenderProfileVersion());
        target.setRenderProfileJson(source.getRenderProfileJson());
        templateVersionRepository.save(target);

        cloneSupport.copyVersionGraph(source, target);
        TemplateLifecycleStatus fromStatus = template.getLifecycleStatus();
        template.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);

        cloneSupport.recordCloneLifecycle(
                template,
                fromStatus,
                releaseVersion,
                target.getId(),
                target.getDevVersionNumber(),
                session
        );

        return new TemplateDevVersionCreatedView(
                target.getId().toString(),
                target.getDevVersionNumber()
        );
    }

    TemplateDetailView abandonInFlightDev(
            TemplateEntity template,
            UUID templateId,
            UUID devVersionId,
            TemplateVersionEntity version,
            ManagementSessionClaims session,
            BiFunction<TemplateEntity, ManagementSessionClaims, TemplateDetailView> toDetail
    ) {
        if (!templateCurrentVersionResolver.isInFlight(version)) {
            throw new TemplateGovernanceException(
                    ApiErrorCodes.TEMPLATE_VERSION_IMMUTABLE,
                    "api.error.template.versionImmutable",
                    HttpStatus.FORBIDDEN
            );
        }
        TemplateLifecycleStatus fromStatus = template.getLifecycleStatus();

        version.setDeletedAt(Instant.now());
        templateVersionRepository.save(version);
        TemplateLifecycleStatus toStatus = templateCurrentVersionResolver.findLatestPublishedVersion(templateId)
                .map(TemplateVersionEntity::getLifecycleStatus)
                .orElse(TemplateLifecycleStatus.DRAFT);
        template.setLifecycleStatus(toStatus);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);

        cloneSupport.recordAbandonLifecycle(
                template,
                fromStatus,
                toStatus,
                version.getDevVersionNumber(),
                devVersionId,
                session
        );

        return toDetail.apply(template, session);
    }
}
