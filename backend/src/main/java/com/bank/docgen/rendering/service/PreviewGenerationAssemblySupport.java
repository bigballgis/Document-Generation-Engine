package com.bank.docgen.rendering.service;

import com.bank.docgen.authoring.structured.CallerRenderOverride;
import com.bank.docgen.authoring.structured.FidelityValidationService;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.api.FidelityWarningView;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.document.RenderProfile;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.port.RenderableTemplateSnapshot;
import com.bank.docgen.template.port.TemplateRenderContextPort;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Package-private preview DOCX/PDF assembly and artifact storage (rendering-adjacent).
 */
final class PreviewGenerationAssemblySupport {

    private static final EncryptionOptionsView NO_ENCRYPTION =
            new EncryptionOptionsView(false, null, null, null);

    private final AnchorBindingRepository anchorBindingRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocxAssembler docxAssembler;
    private final DocumentArtifactPipeline documentArtifactPipeline;
    private final TemplateRenderContextPort renderContextPort;
    private final RenderProfileService renderProfileService;
    private final FidelityValidationService fidelityValidationService;

    PreviewGenerationAssemblySupport(
            AnchorBindingRepository anchorBindingRepository,
            MasterDocumentRepository masterDocumentRepository,
            ObjectStoragePort objectStoragePort,
            DocxAssembler docxAssembler,
            DocumentArtifactPipeline documentArtifactPipeline,
            TemplateRenderContextPort renderContextPort,
            RenderProfileService renderProfileService,
            FidelityValidationService fidelityValidationService
    ) {
        this.anchorBindingRepository = anchorBindingRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.objectStoragePort = objectStoragePort;
        this.docxAssembler = docxAssembler;
        this.documentArtifactPipeline = documentArtifactPipeline;
        this.renderContextPort = renderContextPort;
        this.renderProfileService = renderProfileService;
        this.fidelityValidationService = fidelityValidationService;
    }

    record AssembledPreview(
            String storageKey,
            String pdfStorageKey,
            List<FidelityWarningView> warnings,
            List<AnchorBindingEntity> bindings
    ) {
    }

    AssembledPreview assembleAndStore(
            RenderableTemplateSnapshot template,
            TemplateVersionEntity version,
            UUID previewId,
            Map<String, Object> variables
    ) throws IOException {
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(template.masterId())
                .orElseThrow(MasterNotFoundException::new);
        List<AnchorBindingEntity> bindings = anchorBindingRepository
                .findByTemplateVersionIdOrderByAnchorIdAsc(version.getId());
        Map<String, String> bindingJson = new LinkedHashMap<>();
        bindings.forEach(binding -> bindingJson.put(binding.getAnchorId(), binding.getStructuredContentJson()));
        Map<String, String> pinnedModuleStructures =
                renderContextPort.resolvePinnedContentStructures(version.getId());
        byte[] docx;
        try (InputStream masterStream = objectStoragePort.get(master.getStorageKey())) {
            docx = docxAssembler.assembleStructured(
                    masterStream,
                    bindingJson,
                    variables,
                    pinnedModuleStructures
            );
        }
        String storageKey = "previews/" + previewId + "/output.docx";
        objectStoragePort.put(
                storageKey,
                new ByteArrayInputStream(docx),
                docx.length,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        RenderProfile renderProfile = renderProfileService.resolveEffectiveProfile(
                version,
                CallerRenderOverride.empty()
        );
        DocumentArtifactPipeline.GeneratedArtifact pdfArtifact = documentArtifactPipeline.finalizeArtifact(
                docx,
                "PDF",
                NO_ENCRYPTION,
                renderProfile
        );
        String pdfStorageKey = "previews/" + previewId + "/output.pdf";
        try (pdfArtifact) {
            try (InputStream pdfStream = pdfArtifact.spooled().openInputStream()) {
                objectStoragePort.put(
                        pdfStorageKey,
                        pdfStream,
                        pdfArtifact.spooled().sizeBytes(),
                        pdfArtifact.contentType()
                );
            }
        }
        List<FidelityWarningView> warnings = fidelityValidationService.collectWarningsForVersion(
                version.getId(),
                template.masterId()
        );
        return new AssembledPreview(storageKey, pdfStorageKey, warnings, bindings);
    }
}
