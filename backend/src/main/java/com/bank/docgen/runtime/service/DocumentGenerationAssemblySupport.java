package com.bank.docgen.runtime.service;

import com.bank.docgen.authoring.structured.CallerRenderOverride;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocxAssemblyException;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.RenderingOperationException;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.document.RenderProfile;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateContentModuleReferenceService;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.VersionFidelityWarningService;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Package-private DOCX assembly + artifact finalize/store for runtime generation (rendering-adjacent).
 */
final class DocumentGenerationAssemblySupport {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentGenerationAssemblySupport.class);

    private final TemplateVersionRepository templateVersionRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocxAssembler docxAssembler;
    private final DocumentArtifactPipeline documentArtifactPipeline;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final RenderProfileService renderProfileService;
    private final VersionFidelityWarningService versionFidelityWarningService;

    DocumentGenerationAssemblySupport(
            TemplateVersionRepository templateVersionRepository,
            AnchorBindingRepository anchorBindingRepository,
            MasterDocumentRepository masterDocumentRepository,
            ObjectStoragePort objectStoragePort,
            DocxAssembler docxAssembler,
            DocumentArtifactPipeline documentArtifactPipeline,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            RenderProfileService renderProfileService,
            VersionFidelityWarningService versionFidelityWarningService
    ) {
        this.templateVersionRepository = templateVersionRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.objectStoragePort = objectStoragePort;
        this.docxAssembler = docxAssembler;
        this.documentArtifactPipeline = documentArtifactPipeline;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.renderProfileService = renderProfileService;
        this.versionFidelityWarningService = versionFidelityWarningService;
    }

    DocumentGenerationEngine.GeneratedDocument generate(
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
        } catch (DocxAssemblyException ex) {
            LOG.warn("Document generation assembly failed for template {}: {}", template.getId(), ex.getMessage());
            throw ex;
        } catch (IOException | RuntimeException ex) {
            LOG.warn("Document generation assembly failed for template {}: {}", template.getId(), ex.getMessage());
            throw new RenderingOperationException("api.error.rendering.generationFailed");
        }
        DocumentArtifactPipeline.GeneratedArtifact artifact = documentArtifactPipeline.finalizeArtifact(
                docx,
                outputFormat,
                encryption,
                renderProfile
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
            List<String> fidelityWarnings = new ArrayList<>(
                    versionFidelityWarningService.resolveWarningCodes(version, template.getMasterId())
            );
            fidelityWarnings.addAll(artifact.pipelineWarningCodes());
            return new DocumentGenerationEngine.GeneratedDocument(
                    documentId,
                    storageKey,
                    null,
                    artifact.contentType(),
                    outputFormat,
                    List.copyOf(fidelityWarnings)
            );
        } catch (IOException ex) {
            throw new RenderingOperationException("api.error.rendering.generationFailed");
        }
    }
}
