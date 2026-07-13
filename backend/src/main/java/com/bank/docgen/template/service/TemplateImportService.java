package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ImportTemplateRequest;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.api.TemplateImportResult;
import com.bank.docgen.template.api.TemplateImportSummaryView;
import com.bank.docgen.template.domain.TemplateImportConflictPolicy;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateImportService {

    private final TemplateRepository templateRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final TemplateService templateService;
    private final ManagementAuditRecorder managementAuditRecorder;
    private final TemplateExportAccessService importAccessSupport;
    private final TemplateImportBundleValidator bundleValidator;
    private final TemplateImportTargetResolutionSupport targetResolution;
    private final TemplateImportApplySupport applySupport;

    public TemplateImportService(
            TemplateRepository templateRepository,
            TemplateVersionRepository templateVersionRepository,
            MasterDocumentRepository masterDocumentRepository,
            ApiPolicyRepository apiPolicyRepository,
            TemplateService templateService,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            ManagementAuditRecorder managementAuditRecorder,
            TemplateExportAccessService importAccessSupport,
            TemplateImportBundleValidator bundleValidator,
            ObjectMapper objectMapper,
            TemplateCurrentVersionResolver templateCurrentVersionResolver
    ) {
        this.templateRepository = templateRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.templateService = templateService;
        this.managementAuditRecorder = managementAuditRecorder;
        this.importAccessSupport = importAccessSupport;
        this.bundleValidator = bundleValidator;
        this.targetResolution = new TemplateImportTargetResolutionSupport(
                templateRepository,
                templateVersionRepository,
                importAccessSupport,
                templateCurrentVersionResolver
        );
        this.applySupport = new TemplateImportApplySupport(
                templateService,
                contentModuleReferenceService,
                apiPolicyRepository,
                objectMapper
        );
    }

    @Transactional
    public TemplateImportResult importBundle(ImportTemplateRequest request, ManagementSessionClaims session) {
        TemplateExportBundleView bundle = request.bundle();
        bundleValidator.validate(bundle);
        TemplateExportMetadataView metadata = bundle.metadata();
        importAccessSupport.assertCanImportForGroup(metadata.groupCode(), session);

        TemplateImportConflictPolicy conflictPolicy = request.importConflictPolicy() == null
                ? TemplateImportConflictPolicy.REJECT_IMPORT
                : request.importConflictPolicy();
        UUID sourceTemplateId = UUID.fromString(metadata.templateId());
        String importBatchId = UUID.randomUUID().toString();

        Optional<TemplateEntity> existingById = templateRepository.findByIdAndDeletedAtIsNull(sourceTemplateId);
        Optional<TemplateEntity> existingByExternalId =
                templateRepository.findByExternalIdAndDeletedAtIsNull(metadata.externalId());

        ImportTarget target = targetResolution.resolveImportTarget(
                sourceTemplateId,
                metadata,
                UUID.fromString(request.masterId()),
                conflictPolicy,
                existingById,
                existingByExternalId,
                session
        );

        UUID targetMasterId = UUID.fromString(request.masterId());
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(targetMasterId)
                .orElseThrow(MasterNotFoundException::new);
        targetResolution.assertMasterCompatible(master, metadata);

        applySupport.applyBundleArtifacts(target.templateId(), bundle, session);
        applySupport.applyPolicySnapshot(target.templateId(), bundle.policySnapshot(), session);

        TemplateEntity savedTemplate = templateRepository.findByIdAndDeletedAtIsNull(target.templateId())
                .orElseThrow(TemplateNotFoundException::new);
        managementAuditRecorder.recordTemplateImported(
                savedTemplate.getId(),
                savedTemplate.getGroupCode(),
                savedTemplate.getExternalId(),
                importBatchId,
                target.devVersionNumber(),
                session.username(),
                session.displayName()
        );

        return new TemplateImportResult(
                new TemplateImportSummaryView(
                        savedTemplate.getId().toString(),
                        target.devVersionNumber(),
                        importBatchId
                ),
                templateService.toDetail(savedTemplate)
        );
    }

    record ImportTarget(UUID templateId, int devVersionNumber) {
    }
}
