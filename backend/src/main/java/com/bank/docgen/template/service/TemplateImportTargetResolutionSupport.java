package com.bank.docgen.template.service;

import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.domain.TemplateImportConflictPolicy;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Package-private import target resolution / reset for TemplateImportService.
 */
final class TemplateImportTargetResolutionSupport {

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateExportAccessService importAccessSupport;
    private final TemplateCurrentVersionResolver templateCurrentVersionResolver;

    TemplateImportTargetResolutionSupport(
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            TemplateExportAccessService importAccessSupport,
            TemplateCurrentVersionResolver templateCurrentVersionResolver
    ) {
        this.templateRepository = templateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.importAccessSupport = importAccessSupport;
        this.templateCurrentVersionResolver = templateCurrentVersionResolver;
    }

    TemplateImportService.ImportTarget resolveImportTarget(
            UUID sourceTemplateId,
            TemplateExportMetadataView metadata,
            UUID targetMasterId,
            TemplateImportConflictPolicy conflictPolicy,
            Optional<TemplateEntity> existingById,
            Optional<TemplateEntity> existingByExternalId,
            ManagementSessionClaims session
    ) {
        if (existingById.isPresent()) {
            if (conflictPolicy == TemplateImportConflictPolicy.REJECT_IMPORT) {
                throw new TemplateValidationException("api.error.template.importConflict");
            }
            TemplateEntity template = existingById.get();
            importAccessSupport.assertCanExport(template, session);
            return resetExistingTemplate(template, metadata, targetMasterId, session);
        }
        if (existingByExternalId.isPresent()) {
            throw new TemplateValidationException("api.error.template.importConflict");
        }
        TemplateEntity template = new TemplateEntity(
                sourceTemplateId,
                metadata.externalId(),
                metadata.groupCode(),
                metadata.name(),
                metadata.description(),
                targetMasterId,
                session.username()
        );
        applyLocaleMetadata(template, metadata);
        templateRepository.save(template);
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), sourceTemplateId, session.username());
        templateVersionRepository.save(version);
        return new TemplateImportService.ImportTarget(sourceTemplateId, version.getDevVersionNumber());
    }

    TemplateImportService.ImportTarget resetExistingTemplate(
            TemplateEntity template,
            TemplateExportMetadataView metadata,
            UUID targetMasterId,
            ManagementSessionClaims session
    ) {
        template.setName(metadata.name());
        template.setDescription(metadata.description());
        template.setMasterId(targetMasterId);
        applyLocaleMetadata(template, metadata);
        template.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        template.setReleaseVersion(null);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);

        TemplateVersionEntity version = templateCurrentVersionResolver.requireLatestVersion(template.getId());
        version.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        version.setReleaseVersion(null);
        templateVersionRepository.save(version);
        return new TemplateImportService.ImportTarget(template.getId(), version.getDevVersionNumber());
    }

    void assertMasterCompatible(MasterDocumentEntity master, TemplateExportMetadataView metadata) {
        assertMasterCompatible(master, metadata, false);
    }

    /**
     * @param allowDraftMaterialized Wave 7 P2 — pack-materialized letterhead may be DRAFT only
     */
    void assertMasterCompatible(
            MasterDocumentEntity master,
            TemplateExportMetadataView metadata,
            boolean allowDraftMaterialized
    ) {
        boolean approved = master.getStatus() == MasterDocumentStatus.APPROVED;
        boolean draftOk = allowDraftMaterialized && master.getStatus() == MasterDocumentStatus.DRAFT;
        if (!approved && !draftOk) {
            throw new TemplateValidationException("api.error.template.masterNotApproved");
        }
        if (!master.getGroupCode().equals(metadata.groupCode())) {
            throw new TemplateValidationException("api.error.template.masterGroupMismatch");
        }
    }

    private static void applyLocaleMetadata(TemplateEntity template, TemplateExportMetadataView metadata) {
        String locale = metadata.locale() == null || metadata.locale().isBlank()
                ? "zh-CN"
                : metadata.locale().trim();
        template.setLocale(locale);
        if (metadata.localeVariantFamilyId() != null && !metadata.localeVariantFamilyId().isBlank()) {
            template.setLocaleVariantFamilyId(UUID.fromString(metadata.localeVariantFamilyId()));
        }
    }
}
