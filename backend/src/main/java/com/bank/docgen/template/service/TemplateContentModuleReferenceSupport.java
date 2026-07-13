package com.bank.docgen.template.service;

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
import java.util.Locale;

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

    ContentModuleReferenceView toView(TemplateContentModuleReferenceEntity reference) {
        ContentModuleVersionEntity version = contentModuleVersionRepository
                .findById(reference.getContentModuleVersionId())
                .orElseThrow(ContentModuleNotFoundException::new);
        ContentModuleEntity module = contentModuleRepository.findByIdAndDeletedAtIsNull(version.getModuleId())
                .orElseThrow(ContentModuleNotFoundException::new);
        return new ContentModuleReferenceView(
                reference.getReferenceKey(),
                contentModuleAccessService.publicModuleId(module),
                version.getSemanticVersion(),
                reference.isLockedFlag()
        );
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
}
