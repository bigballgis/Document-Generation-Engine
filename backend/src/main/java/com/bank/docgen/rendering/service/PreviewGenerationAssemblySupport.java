package com.bank.docgen.rendering.service;

import com.bank.docgen.authoring.structured.CallerRenderOverride;
import com.bank.docgen.authoring.structured.FidelityValidationService;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.documentbrand.domain.ResolvedDocumentBrand;
import com.bank.docgen.documentbrand.service.DocumentBrandResolveService;
import com.bank.docgen.documentbrand.service.DocumentBrandSlotApplicationSupport;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocxSpecimenWatermarkStamper;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.PaginationDeltaFidelitySupport;
import com.bank.docgen.rendering.PdfSpecimenWatermarkStamper;
import com.bank.docgen.rendering.api.FidelityWarningView;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.document.RenderProfile;
import com.bank.docgen.sharedkernel.document.fidelity.PaginationDeltaEvaluator;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.port.RenderableTemplateSnapshot;
import com.bank.docgen.template.port.TemplateRenderContextPort;
import com.bank.docgen.template.port.VariableComputePort;
import com.bank.docgen.template.port.VariableSchemaValidationPort;
import com.bank.docgen.template.port.CompositionInclusionAxes;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
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
    private final VariableComputePort variableComputePort;
    private final VariableSchemaValidationPort variableSchemaValidationPort;
    private final PaginationDeltaFidelitySupport paginationDeltaFidelitySupport;
    private final DocumentBrandResolveService documentBrandResolveService;

    PreviewGenerationAssemblySupport(
            AnchorBindingRepository anchorBindingRepository,
            MasterDocumentRepository masterDocumentRepository,
            ObjectStoragePort objectStoragePort,
            DocxAssembler docxAssembler,
            DocumentArtifactPipeline documentArtifactPipeline,
            TemplateRenderContextPort renderContextPort,
            RenderProfileService renderProfileService,
            FidelityValidationService fidelityValidationService,
            VariableComputePort variableComputePort,
            VariableSchemaValidationPort variableSchemaValidationPort,
            PaginationDeltaFidelitySupport paginationDeltaFidelitySupport,
            DocumentBrandResolveService documentBrandResolveService
    ) {
        this.anchorBindingRepository = anchorBindingRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.objectStoragePort = objectStoragePort;
        this.docxAssembler = docxAssembler;
        this.documentArtifactPipeline = documentArtifactPipeline;
        this.renderContextPort = renderContextPort;
        this.renderProfileService = renderProfileService;
        this.fidelityValidationService = fidelityValidationService;
        this.variableComputePort = variableComputePort;
        this.variableSchemaValidationPort = variableSchemaValidationPort;
        this.paginationDeltaFidelitySupport = paginationDeltaFidelitySupport;
        this.documentBrandResolveService = documentBrandResolveService;
    }

    record AssembledPreview(
            String storageKey,
            String pdfStorageKey,
            List<FidelityWarningView> warnings,
            List<AnchorBindingEntity> bindings,
            Integer pdfPageCount
    ) {
    }

    AssembledPreview assembleAndStore(
            RenderableTemplateSnapshot template,
            TemplateVersionEntity version,
            UUID previewId,
            Map<String, Object> variables
    ) throws IOException {
        return assembleAndStore(template, version, previewId, variables, null, CompositionInclusionAxes.empty());
    }

    AssembledPreview assembleAndStore(
            RenderableTemplateSnapshot template,
            TemplateVersionEntity version,
            UUID previewId,
            Map<String, Object> variables,
            String localeTag
    ) throws IOException {
        return assembleAndStore(
                template,
                version,
                previewId,
                variables,
                localeTag,
                CompositionInclusionAxes.empty()
        );
    }

    AssembledPreview assembleAndStore(
            RenderableTemplateSnapshot template,
            TemplateVersionEntity version,
            UUID previewId,
            Map<String, Object> variables,
            String localeTag,
            CompositionInclusionAxes inclusionAxes
    ) throws IOException {
        return assembleAndStore(template, version, previewId, variables, localeTag, inclusionAxes, null);
    }

    AssembledPreview assembleAndStore(
            RenderableTemplateSnapshot template,
            TemplateVersionEntity version,
            UUID previewId,
            Map<String, Object> variables,
            String localeTag,
            CompositionInclusionAxes inclusionAxes,
            String legalEntityCode
    ) throws IOException {
        // IBL-A1: preview aligned with runtime — fail-closed before compute/assemble.
        variableSchemaValidationPort.validateForAssembly(version.getId(), variables);
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(template.masterId())
                .orElseThrow(MasterNotFoundException::new);
        Map<String, Object> resolvedVariables = variableComputePort.applyCompute(
                version.getId(),
                variables,
                localeTag
        );
        // IBL-E4 / ADR-0065: same document-brand resolve/apply as runtime.
        ResolvedDocumentBrand resolvedBrand = documentBrandResolveService.resolve(
                template.groupCode(),
                legalEntityCode,
                template.allowedDocumentBrandCodes()
        );
        List<AnchorBindingEntity> bindings = anchorBindingRepository
                .findByTemplateVersionIdOrderByAnchorIdAsc(version.getId());
        Map<String, String> sourceBindings = new LinkedHashMap<>();
        bindings.forEach(binding -> sourceBindings.put(binding.getAnchorId(), binding.getStructuredContentJson()));
        DocumentBrandSlotApplicationSupport.Applied brandApplied =
                DocumentBrandSlotApplicationSupport.apply(sourceBindings, resolvedBrand);
        Map<String, String> bindingJson = new LinkedHashMap<>(brandApplied.bindingJson());
        // ADR-0063 / E2-C9: same evaluator + real axes as runtime generate.
        Map<String, String> pinnedModuleStructures =
                renderContextPort.resolvePinnedContentStructures(
                        version.getId(),
                        inclusionAxes == null ? CompositionInclusionAxes.empty() : inclusionAxes
                );
        byte[] docx;
        try (InputStream masterStream = objectStoragePort.get(master.getStorageKey())) {
            docx = docxAssembler.assembleStructured(
                    masterStream,
                    bindingJson,
                    resolvedVariables,
                    pinnedModuleStructures
            );
        }
        // CE-G02: SPECIMEN watermark — preview path only (fail-closed).
        docx = DocxSpecimenWatermarkStamper.apply(docx);
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
        Integer pdfPageCount;
        try (pdfArtifact) {
            byte[] pdfBytes;
            try (InputStream pdfStream = pdfArtifact.spooled().openInputStream()) {
                pdfBytes = pdfStream.readAllBytes();
            }
            pdfBytes = PdfSpecimenWatermarkStamper.apply(pdfBytes);
            pdfPageCount = paginationDeltaFidelitySupport.measurePdfPages(pdfBytes);
            objectStoragePort.put(
                    pdfStorageKey,
                    new ByteArrayInputStream(pdfBytes),
                    pdfBytes.length,
                    pdfArtifact.contentType()
            );
        }
        List<FidelityWarningView> warnings = new ArrayList<>(
                fidelityValidationService.collectWarningsForVersion(
                        version.getId(),
                        template.masterId()
                )
        );
        for (String code : brandApplied.fidelityWarningCodes()) {
            warnings.add(new FidelityWarningView(code, "api.fidelity.documentBrandSlotsAbsent"));
        }
        PaginationDeltaEvaluator.Evaluation paginationEval = paginationDeltaFidelitySupport.evaluate(
                version.getAuthorWordPageCount(),
                pdfPageCount
        );
        paginationDeltaFidelitySupport.warningIfNeeded(paginationEval).ifPresent(warnings::add);
        return new AssembledPreview(storageKey, pdfStorageKey, warnings, bindings, pdfPageCount);
    }
}
