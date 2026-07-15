package com.bank.docgen.template.service;

import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.service.ContentModuleAccessService;
import com.bank.docgen.contentmodule.service.ContentModuleNotFoundException;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;
import com.bank.docgen.template.persistence.TemplateEntity;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Package-private resolve / validate / view helpers for template content-module references.
 */
final class TemplateContentModuleReferenceSupport {

    private final ContentModuleRepository contentModuleRepository;
    private final ContentModuleVersionRepository contentModuleVersionRepository;
    private final ContentModuleAccessService contentModuleAccessService;

    TemplateContentModuleReferenceSupport(
            ContentModuleRepository contentModuleRepository,
            ContentModuleVersionRepository contentModuleVersionRepository,
            ContentModuleAccessService contentModuleAccessService
    ) {
        this.contentModuleRepository = contentModuleRepository;
        this.contentModuleVersionRepository = contentModuleVersionRepository;
        this.contentModuleAccessService = contentModuleAccessService;
    }

    boolean isReferenceValidForPublish(TemplateContentModuleReferenceEntity reference) {
        return contentModuleVersionRepository.findById(reference.getContentModuleVersionId())
                .map(version -> isReferenceVersionValidForPublish(reference, version))
                .orElse(false);
    }

    ContentModuleVersionEntity resolveReferencableVersion(
            TemplateEntity template,
            String moduleId,
            String semanticVersion,
            ManagementSessionClaims session
    ) {
        ContentModuleEntity module = contentModuleAccessService.requireReadableModule(moduleId, session);
        assertTemplateCanReferenceModule(template, module);
        ContentModuleVersionEntity version = contentModuleVersionRepository
                .findByModuleIdAndSemanticVersion(module.getId(), semanticVersion.trim())
                .orElseThrow(() -> new TemplateValidationException("api.error.template.contentModuleReferenceMissing"));
        if (!version.isReferencable()) {
            throw new TemplateValidationException("api.error.template.contentModuleReferenceInvalid");
        }
        return version;
    }

    /**
     * CE-E01 import-time seam: allow wiring template refs to DRAFT module versions
     * materialized during the same import transaction. Publish gate still requires
     * {@link ContentModuleVersionEntity#isReferencable()}.
     */
    ContentModuleVersionEntity resolveVersionForImport(
            TemplateEntity template,
            String moduleId,
            String semanticVersion,
            ManagementSessionClaims session
    ) {
        ContentModuleEntity module = contentModuleAccessService.requireReadableModule(moduleId, session);
        assertTemplateCanReferenceModule(template, module);
        ContentModuleVersionEntity version = contentModuleVersionRepository
                .findByModuleIdAndSemanticVersion(module.getId(), semanticVersion.trim())
                .orElseThrow(() -> new TemplateValidationException("api.error.template.contentModuleReferenceMissing"));
        if (isPinnedStructureMissing(version)) {
            throw new TemplateValidationException("api.error.template.contentModuleReferenceInvalid");
        }
        return version;
    }

    ContentModuleReferenceView toView(TemplateContentModuleReferenceEntity reference) {
        ContentModuleVersionEntity version = contentModuleVersionRepository
                .findById(reference.getContentModuleVersionId())
                .orElseThrow(ContentModuleNotFoundException::new);
        ContentModuleEntity module = contentModuleRepository.findByIdAndDeletedAtIsNull(version.getModuleId())
                .orElseThrow(ContentModuleNotFoundException::new);
        Optional<ContentModuleVersionEntity> latestReferencable =
                findLatestReferencableApprovedVersion(version.getModuleId());
        boolean outOfDate = isOutOfDate(version, latestReferencable);
        String latestApprovedSemanticVersion = outOfDate
                ? latestReferencable.map(ContentModuleVersionEntity::getSemanticVersion).orElse(null)
                : null;
        return new ContentModuleReferenceView(
                reference.getReferenceKey(),
                contentModuleAccessService.publicModuleId(module),
                version.getSemanticVersion(),
                reference.isLockedFlag(),
                outOfDate,
                latestApprovedSemanticVersion
        );
    }

    int countOutdatedUnlockedReferences(List<TemplateContentModuleReferenceEntity> references) {
        int count = 0;
        for (TemplateContentModuleReferenceEntity reference : references) {
            if (reference.isLockedFlag()) {
                continue;
            }
            ContentModuleVersionEntity pinned = contentModuleVersionRepository
                    .findById(reference.getContentModuleVersionId())
                    .orElse(null);
            if (pinned == null) {
                continue;
            }
            Optional<ContentModuleVersionEntity> latestReferencable =
                    findLatestReferencableApprovedVersion(pinned.getModuleId());
            if (isOutOfDate(pinned, latestReferencable)) {
                count++;
            }
        }
        return count;
    }

    Optional<ContentModuleVersionEntity> findLatestReferencableApprovedVersion(UUID moduleId) {
        List<ContentModuleVersionEntity> versions = contentModuleVersionRepository
                .findByModuleIdAndReviewStateAndLifecycleStateOrderBySemanticVersionDesc(
                        moduleId,
                        ContentModuleReviewState.APPROVED,
                        ContentModuleLifecycleState.ACTIVE
                );
        return versions.isEmpty() ? Optional.empty() : Optional.of(versions.getFirst());
    }

    private boolean isOutOfDate(
            ContentModuleVersionEntity pinnedVersion,
            Optional<ContentModuleVersionEntity> latestReferencable
    ) {
        return latestReferencable
                .map(latest -> !latest.getId().equals(pinnedVersion.getId()))
                .orElse(false);
    }

    String normalizeReferenceKey(String referenceKey) {
        return referenceKey.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isReferenceVersionValidForPublish(
            TemplateContentModuleReferenceEntity reference,
            ContentModuleVersionEntity version
    ) {
        if (isPinnedStructureMissing(version)) {
            return false;
        }
        if (reference.isLockedFlag()) {
            return true;
        }
        return version.isReferencable();
    }

    private boolean isPinnedStructureMissing(ContentModuleVersionEntity version) {
        String structure = version.getContentStructureJson();
        return structure == null || structure.isBlank();
    }

    private void assertTemplateCanReferenceModule(TemplateEntity template, ContentModuleEntity module) {
        if (template.getGroupCode().equalsIgnoreCase(module.getGroupCode())) {
            return;
        }
        boolean shared = contentModuleAccessService.readSharedGroupCodes(module).stream()
                .anyMatch(code -> code.equalsIgnoreCase(template.getGroupCode()));
        if (!shared) {
            throw new TemplateValidationException("api.error.template.contentModuleReferenceInvalid");
        }
    }

    String formatExpiredEffectiveDetail(ContentModuleVersionEntity version) {
        ContentModuleEntity module = contentModuleRepository.findByIdAndDeletedAtIsNull(version.getModuleId())
                .orElse(null);
        String moduleCode = module == null ? version.getModuleId().toString() : module.getModuleCode();
        return moduleCode
                + "@"
                + version.getSemanticVersion()
                + " effectiveTo="
                + version.getEffectiveTo();
    }
}
