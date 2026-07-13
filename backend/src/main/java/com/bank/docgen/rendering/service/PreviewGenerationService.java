package com.bank.docgen.rendering.service;

import com.bank.docgen.authoring.structured.CallerRenderOverride;
import com.bank.docgen.authoring.structured.FidelityValidationService;
import com.bank.docgen.sharedkernel.document.RenderProfile;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.api.FidelityWarningView;
import com.bank.docgen.rendering.api.PreviewRecordView;
import com.bank.docgen.rendering.api.PreviewSummaryView;
import com.bank.docgen.rendering.api.TestGenerateRequest;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.port.RenderableTemplateSnapshot;
import com.bank.docgen.template.port.TemplatePreviewAuthorizationPort;
import com.bank.docgen.template.port.TemplateRenderContextPort;
import com.bank.docgen.template.port.TestDataSetEvidencePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreviewGenerationService {

    private static final Logger LOG = LoggerFactory.getLogger(PreviewGenerationService.class);
    private static final EncryptionOptionsView NO_ENCRYPTION =
            new EncryptionOptionsView(false, null, null, null);
    private static final int PREVIEW_HISTORY_LIMIT = 50;

    private final TemplatePreviewAuthorizationPort previewAuthorizationPort;
    private final TestDataSetEvidencePort testDataSetEvidencePort;
    private final TemplateRenderContextPort renderContextPort;
    private final AnchorBindingRepository anchorBindingRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final PreviewRecordRepository previewRecordRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocxAssembler docxAssembler;
    private final DocumentArtifactPipeline documentArtifactPipeline;
    private final RenderProfileService renderProfileService;
    private final FidelityValidationService fidelityValidationService;
    private final PreviewRecordMappingSupport mapping;

    public PreviewGenerationService(
            TemplatePreviewAuthorizationPort previewAuthorizationPort,
            TestDataSetEvidencePort testDataSetEvidencePort,
            TemplateRenderContextPort renderContextPort,
            AnchorBindingRepository anchorBindingRepository,
            MasterDocumentRepository masterDocumentRepository,
            PreviewRecordRepository previewRecordRepository,
            ObjectStoragePort objectStoragePort,
            DocxAssembler docxAssembler,
            DocumentArtifactPipeline documentArtifactPipeline,
            ObjectMapper objectMapper,
            PreviewComparisonService previewComparisonService,
            RenderProfileService renderProfileService,
            FidelityValidationService fidelityValidationService
    ) {
        this.previewAuthorizationPort = previewAuthorizationPort;
        this.testDataSetEvidencePort = testDataSetEvidencePort;
        this.renderContextPort = renderContextPort;
        this.anchorBindingRepository = anchorBindingRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.previewRecordRepository = previewRecordRepository;
        this.objectStoragePort = objectStoragePort;
        this.docxAssembler = docxAssembler;
        this.documentArtifactPipeline = documentArtifactPipeline;
        this.renderProfileService = renderProfileService;
        this.fidelityValidationService = fidelityValidationService;
        this.mapping = new PreviewRecordMappingSupport(
                previewComparisonService,
                anchorBindingRepository,
                testDataSetEvidencePort,
                objectMapper
        );
    }

    @Transactional
    public PreviewRecordView testGenerate(
            UUID templateId,
            TestGenerateRequest request,
            ManagementSessionClaims session
    ) {
        return runTestGenerate(templateId, request, null, null, session, true);
    }

    /**
     * Async preview path — persist under the orchestrator-allocated {@code previewId}
     * so SSE stream URLs and artifact download URLs resolve to the same record.
     */
    @Transactional
    public PreviewRecordView testGenerate(
            UUID templateId,
            TestGenerateRequest request,
            UUID previewId,
            ManagementSessionClaims session
    ) {
        return runTestGenerate(templateId, request, null, previewId, session, true);
    }

    @Transactional
    public PreviewRecordView runTestGenerateForBatch(
            UUID templateId,
            String testDataSetId,
            UUID batchTestRunId,
            ManagementSessionClaims session
    ) {
        return runTestGenerate(
                templateId,
                new TestGenerateRequest(null, testDataSetId),
                batchTestRunId,
                null,
                session,
                false
        );
    }

    private PreviewRecordView runTestGenerate(
            UUID templateId,
            TestGenerateRequest request,
            UUID batchTestRunId,
            UUID previewId,
            ManagementSessionClaims session,
            boolean throwOnFailure
    ) {
        RenderableTemplateSnapshot template = previewAuthorizationPort.requireReadableSnapshot(templateId, session);
        Map<String, Object> variables = mapping.resolveVariables(templateId, request, session);
        TemplateVersionEntity version = renderContextPort.requireInFlightDevVersion(templateId);
        String variablesHash = mapping.hashVariables(variables);
        PreviewRecordEntity preview = new PreviewRecordEntity(
                previewId != null ? previewId : UUID.randomUUID(),
                templateId,
                version.getId(),
                "DOCX",
                variablesHash,
                session.username(),
                request.testDataSetId(),
                batchTestRunId
        );
        preview.markProcessing();
        renderProfileService.applyPreviewRenderProfileVersion(preview, version);
        previewRecordRepository.save(preview);
        try {
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
            String storageKey = "previews/" + preview.getId() + "/output.docx";
            objectStoragePort.put(
                    storageKey,
                    new java.io.ByteArrayInputStream(docx),
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
            String pdfStorageKey = "previews/" + preview.getId() + "/output.pdf";
            try (pdfArtifact) {
                try (java.io.InputStream pdfStream = pdfArtifact.spooled().openInputStream()) {
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
            preview.markSucceeded(storageKey, pdfStorageKey, mapping.writeWarnings(warnings));
            previewRecordRepository.save(preview);
            if (request.testDataSetId() != null && !request.testDataSetId().isBlank()) {
                testDataSetEvidencePort.lockForEvidence(templateId, request.testDataSetId());
            }
            return mapping.toView(preview, warnings, bindings);
        } catch (IOException | RuntimeException ex) {
            LOG.warn("Preview generation failed for template {} preview {}: {}", templateId, preview.getId(), ex.getMessage());
            preview.markFailed();
            previewRecordRepository.save(preview);
            if (throwOnFailure) {
                throw new PreviewGenerationException("api.error.rendering.generationFailed", ex);
            }
            return mapping.toView(preview, List.of(), List.of());
        }
    }

    @Transactional(readOnly = true)
    public List<PreviewSummaryView> listPreviews(UUID templateId, ManagementSessionClaims session) {
        previewAuthorizationPort.requireReadableSnapshot(templateId, session);
        return previewRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId).stream()
                .limit(PREVIEW_HISTORY_LIMIT)
                .map(mapping::toSummaryView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PreviewRecordView getPreview(UUID templateId, UUID previewId, ManagementSessionClaims session) {
        previewAuthorizationPort.requireReadableSnapshot(templateId, session);
        PreviewRecordEntity preview = previewRecordRepository.findById(previewId)
                .orElseThrow(PreviewNotFoundException::new);
        if (!preview.getTemplateId().equals(templateId)) {
            throw new PreviewNotFoundException();
        }
        List<AnchorBindingEntity> bindings = anchorBindingRepository
                .findByTemplateVersionIdOrderByAnchorIdAsc(preview.getTemplateVersionId());
        return mapping.toView(preview, mapping.readWarnings(preview.getFidelityWarningsJson()), bindings);
    }
}
