package com.bank.docgen.runtime.service;

import com.bank.docgen.authoring.structured.CallerRenderOverride;
import com.bank.docgen.authoring.structured.FidelityValidationService;
import com.bank.docgen.authoring.structured.RenderProfile;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocxPdfPageNumberStampPlanResolver;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.PdfConversionStampPlanContext;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateContentModuleReferenceService;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TemplateValidationException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DocumentGenerationEngine {

    private final TemplateVersionRepository templateVersionRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocxAssembler docxAssembler;
    private final DocumentArtifactPipeline documentArtifactPipeline;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final RenderProfileService renderProfileService;
    private final FidelityValidationService fidelityValidationService;

    public DocumentGenerationEngine(
            TemplateVersionRepository templateVersionRepository,
            AnchorBindingRepository anchorBindingRepository,
            MasterDocumentRepository masterDocumentRepository,
            ObjectStoragePort objectStoragePort,
            DocxAssembler docxAssembler,
            DocumentArtifactPipeline documentArtifactPipeline,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            RenderProfileService renderProfileService,
            FidelityValidationService fidelityValidationService
    ) {
        this.templateVersionRepository = templateVersionRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.objectStoragePort = objectStoragePort;
        this.docxAssembler = docxAssembler;
        this.documentArtifactPipeline = documentArtifactPipeline;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.renderProfileService = renderProfileService;
        this.fidelityValidationService = fidelityValidationService;
    }

    public GeneratedDocument generate(
            TemplateEntity template,
            String releaseVersion,
            Map<String, Object> variables,
            String outputFormat,
            EncryptionOptionsView encryption
    ) {
        return generate(template, releaseVersion, variables, outputFormat, encryption, CallerRenderOverride.empty());
    }

    public GeneratedDocument generate(
            TemplateEntity template,
            String releaseVersion,
            Map<String, Object> variables,
            String outputFormat,
            EncryptionOptionsView encryption,
            CallerRenderOverride callerRenderOverride
    ) {
        TemplateVersionEntity version = templateVersionRepository
                .findByTemplateIdAndReleaseVersion(template.getId(), releaseVersion)
                .orElseThrow(TemplateNotFoundException::new);
        RenderProfile renderProfile = renderProfileService.resolveEffectiveProfile(
                version,
                callerRenderOverride == null ? CallerRenderOverride.empty() : callerRenderOverride
        );
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(template.getMasterId())
                .orElseThrow(TemplateNotFoundException::new);
        List<AnchorBindingEntity> bindings = anchorBindingRepository
                .findByTemplateVersionIdOrderByAnchorIdAsc(version.getId());
        Map<String, String> bindingJson = new LinkedHashMap<>();
        bindings.forEach(binding -> bindingJson.put(binding.getAnchorId(), binding.getStructuredContentJson()));
        Map<String, String> pinnedModuleStructures =
                contentModuleReferenceService.resolvePinnedContentStructures(version.getId());
        byte[] docx;
        try (InputStream masterStream = objectStoragePort.get(master.getStorageKey())) {
            docx = docxAssembler.assembleStructured(
                    masterStream,
                    bindingJson,
                    variables,
                    pinnedModuleStructures
            );
        } catch (Exception ex) {
            throw new TemplateValidationException("api.error.rendering.generationFailed");
        }
        DocumentArtifactPipeline.GeneratedArtifact artifact = PdfConversionStampPlanContext.runWith(
                DocxPdfPageNumberStampPlanResolver.resolve(docx, renderProfile),
                () -> documentArtifactPipeline.finalizeArtifact(
                        docx,
                        outputFormat,
                        encryption,
                        renderProfile
                )
        );
        try (artifact) {
            String documentId = "DOC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
            String storageKey = "generated/" + documentId + "/" + artifact.storageFileName();
            try (InputStream artifactStream = artifact.spooled().openInputStream()) {
                objectStoragePort.put(
                        storageKey,
                        artifactStream,
                        artifact.spooled().sizeBytes(),
                        artifact.contentType()
                );
            }
            return new GeneratedDocument(
                    documentId,
                    storageKey,
                    null,
                    artifact.contentType(),
                    outputFormat,
                    fidelityValidationService.collectWarningCodesForVersion(version.getId(), template.getMasterId())
            );
        } catch (java.io.IOException ex) {
            throw new TemplateValidationException("api.error.rendering.generationFailed");
        }
    }

    public record GeneratedDocument(
            String documentId,
            String storageKey,
            byte[] artifactBytes,
            String contentType,
            String outputFormat,
            List<String> fidelityWarningCodes
    ) {
    }
}
