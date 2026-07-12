package com.bank.docgen.template.service;

import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.ImportTemplateRequest;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.api.TemplateImportResult;
import com.bank.docgen.template.api.TemplateImportSummaryView;
import com.bank.docgen.template.api.UpsertAnchorBindingRequest;
import com.bank.docgen.template.api.UpsertContentModuleReferenceRequest;
import com.bank.docgen.template.api.UpsertVariableSchemaRequest;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.TemplateImportConflictPolicy;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TemplateImportService {

    private final TemplateRepository templateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final ApiPolicyRepository apiPolicyRepository;
    private final TemplateService templateService;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final ManagementAuditRecorder managementAuditRecorder;
    private final TemplateExportAccessService importAccessSupport;
    private final TemplateImportBundleValidator bundleValidator;
    private final ObjectMapper objectMapper;
    private final TemplateCurrentVersionResolver templateCurrentVersionResolver;

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
        this.templateVersionRepository = templateVersionRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.apiPolicyRepository = apiPolicyRepository;
        this.templateService = templateService;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.managementAuditRecorder = managementAuditRecorder;
        this.importAccessSupport = importAccessSupport;
        this.bundleValidator = bundleValidator;
        this.objectMapper = objectMapper;
        this.templateCurrentVersionResolver = templateCurrentVersionResolver;
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

        ImportTarget target = resolveImportTarget(
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
        assertMasterCompatible(master, metadata);

        applyBundleArtifacts(target.templateId(), bundle, session);
        applyPolicySnapshot(target.templateId(), bundle.policySnapshot(), session);

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

    private ImportTarget resolveImportTarget(
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
            return resetExistingTemplate(template, metadata, session);
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
        templateRepository.save(template);
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), sourceTemplateId, session.username());
        templateVersionRepository.save(version);
        return new ImportTarget(sourceTemplateId, version.getDevVersionNumber());
    }

    private ImportTarget resetExistingTemplate(
            TemplateEntity template,
            TemplateExportMetadataView metadata,
            ManagementSessionClaims session
    ) {
        template.setName(metadata.name());
        template.setDescription(metadata.description());
        template.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        template.setReleaseVersion(null);
        template.setUpdatedBy(session.username());
        templateRepository.save(template);

        TemplateVersionEntity version = templateCurrentVersionResolver.requireLatestVersion(template.getId());
        version.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        version.setReleaseVersion(null);
        templateVersionRepository.save(version);
        return new ImportTarget(template.getId(), version.getDevVersionNumber());
    }

    private void assertMasterCompatible(MasterDocumentEntity master, TemplateExportMetadataView metadata) {
        if (master.getStatus() != MasterDocumentStatus.APPROVED) {
            throw new TemplateValidationException("api.error.template.masterNotApproved");
        }
        if (!master.getGroupCode().equals(metadata.groupCode())) {
            throw new TemplateValidationException("api.error.template.masterGroupMismatch");
        }
    }

    private void applyBundleArtifacts(
            UUID templateId,
            TemplateExportBundleView bundle,
            ManagementSessionClaims session
    ) {
        for (VariableSchemaView variable : bundle.variables()) {
            templateService.upsertVariable(
                    templateId,
                    new UpsertVariableSchemaRequest(
                            variable.variableKey(),
                            variable.variableType(),
                            variable.required(),
                            variable.defaultValue(),
                            variable.enumValues(),
                            variable.description(),
                            variable.computeExpression()
                    ),
                    session
            );
        }
        for (AnchorBindingView binding : bundle.bindings()) {
            templateService.upsertBinding(
                    templateId,
                    new UpsertAnchorBindingRequest(
                            binding.anchorId(),
                            AnchorContentType.valueOf(binding.declaredContentType()),
                            binding.structuredContentJson(),
                            binding.pasteCleaningEvidence(),
                            null
                    ),
                    session
            );
        }
        templateService.saveRules(templateId, bundle.rules(), session);
        for (ContentModuleReferenceView reference : bundle.contentModuleReferences()) {
            if (reference.locked()) {
                continue;
            }
            contentModuleReferenceService.upsertReference(
                    templateId,
                    new UpsertContentModuleReferenceRequest(
                            reference.referenceKey(),
                            reference.moduleId(),
                            reference.semanticVersion()
                    ),
                    session
            );
        }
    }

    private void applyPolicySnapshot(UUID templateId, ApiPolicyView policySnapshot, ManagementSessionClaims session) {
        if (policySnapshot == null) {
            return;
        }
        try {
            String allowedJson = objectMapper.writeValueAsString(policySnapshot.allowedAdGroups());
            String outputFormatsJson = objectMapper.writeValueAsString(policySnapshot.outputFormats());
            String outputModesJson = objectMapper.writeValueAsString(policySnapshot.outputModes());
            Optional<ApiPolicyEntity> existing = apiPolicyRepository.findByTemplateId(templateId);
            ApiPolicyEntity policy;
            if (existing.isPresent()) {
                policy = existing.get();
                policy.replaceConfiguration(
                        allowedJson,
                        policy.getDefaultRouteReleaseVersion(),
                        outputFormatsJson,
                        outputModesJson,
                        policySnapshot.batchEnabled(),
                        policySnapshot.maxBatchSize(),
                        policySnapshot.docxEncryptionEnabled(),
                        policySnapshot.pdfEncryptionEnabled(),
                        session.username()
                );
            } else {
                policy = new ApiPolicyEntity(UUID.randomUUID(), templateId, allowedJson, session.username());
                policy.replaceConfiguration(
                        allowedJson,
                        null,
                        outputFormatsJson,
                        outputModesJson,
                        policySnapshot.batchEnabled(),
                        policySnapshot.maxBatchSize(),
                        policySnapshot.docxEncryptionEnabled(),
                        policySnapshot.pdfEncryptionEnabled(),
                        session.username()
                );
            }
            apiPolicyRepository.save(policy);
        } catch (JsonProcessingException exception) {
            throw new TemplateValidationException("api.error.template.importFailed");
        }
    }

    private record ImportTarget(UUID templateId, int devVersionNumber) {
    }
}
