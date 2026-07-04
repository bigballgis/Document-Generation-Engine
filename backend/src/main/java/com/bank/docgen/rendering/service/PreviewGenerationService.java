package com.bank.docgen.rendering.service;

import com.bank.docgen.authoring.structured.CallerRenderOverride;
import com.bank.docgen.authoring.structured.FidelityValidationService;
import com.bank.docgen.authoring.structured.RenderProfile;
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
import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateContentModuleReferenceService;
import com.bank.docgen.template.service.TemplateCurrentVersionResolver;
import com.bank.docgen.template.service.TemplateService;
import com.bank.docgen.template.service.TestDataSetService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
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

    private final TemplateService templateService;
    private final TestDataSetService testDataSetService;
    private final TemplateVersionRepository templateVersionRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final PreviewRecordRepository previewRecordRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocxAssembler docxAssembler;
    private final DocumentArtifactPipeline documentArtifactPipeline;
    private final ObjectMapper objectMapper;
    private final PreviewComparisonService previewComparisonService;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final RenderProfileService renderProfileService;
    private final FidelityValidationService fidelityValidationService;
    private final TemplateCurrentVersionResolver templateCurrentVersionResolver;

    public PreviewGenerationService(
            TemplateService templateService,
            TestDataSetService testDataSetService,
            TemplateVersionRepository templateVersionRepository,
            AnchorBindingRepository anchorBindingRepository,
            MasterDocumentRepository masterDocumentRepository,
            PreviewRecordRepository previewRecordRepository,
            ObjectStoragePort objectStoragePort,
            DocxAssembler docxAssembler,
            DocumentArtifactPipeline documentArtifactPipeline,
            ObjectMapper objectMapper,
            PreviewComparisonService previewComparisonService,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            RenderProfileService renderProfileService,
            FidelityValidationService fidelityValidationService,
            TemplateCurrentVersionResolver templateCurrentVersionResolver
    ) {
        this.templateService = templateService;
        this.testDataSetService = testDataSetService;
        this.templateVersionRepository = templateVersionRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.previewRecordRepository = previewRecordRepository;
        this.objectStoragePort = objectStoragePort;
        this.docxAssembler = docxAssembler;
        this.documentArtifactPipeline = documentArtifactPipeline;
        this.objectMapper = objectMapper;
        this.previewComparisonService = previewComparisonService;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.renderProfileService = renderProfileService;
        this.fidelityValidationService = fidelityValidationService;
        this.templateCurrentVersionResolver = templateCurrentVersionResolver;
    }

    @Transactional
    public PreviewRecordView testGenerate(
            UUID templateId,
            TestGenerateRequest request,
            ManagementSessionClaims session
    ) {
        return runTestGenerate(templateId, request, null, session, true);
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
                session,
                false
        );
    }

    private PreviewRecordView runTestGenerate(
            UUID templateId,
            TestGenerateRequest request,
            UUID batchTestRunId,
            ManagementSessionClaims session,
            boolean throwOnFailure
    ) {
        TemplateEntity template = templateService.requireReadableTemplate(templateId, session);
        Map<String, Object> variables = resolveVariables(templateId, request, session);
        TemplateVersionEntity version = templateCurrentVersionResolver.requireInFlightDevVersion(templateId);
        String variablesHash = hashVariables(variables);
        PreviewRecordEntity preview = new PreviewRecordEntity(
                UUID.randomUUID(),
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
            MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(template.getMasterId())
                    .orElseThrow(MasterNotFoundException::new);
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
                    template.getMasterId()
            );
            preview.markSucceeded(storageKey, pdfStorageKey, writeWarnings(warnings));
            previewRecordRepository.save(preview);
            if (request.testDataSetId() != null && !request.testDataSetId().isBlank()) {
                testDataSetService.lockForEvidence(templateId, request.testDataSetId());
            }
            return toView(preview, warnings, bindings);
        } catch (IOException | RuntimeException ex) {
            LOG.warn("Preview generation failed for template {} preview {}: {}", templateId, preview.getId(), ex.getMessage());
            preview.markFailed();
            previewRecordRepository.save(preview);
            if (throwOnFailure) {
                throw new PreviewGenerationException("api.error.rendering.generationFailed", ex);
            }
            return toView(preview, List.of(), List.of());
        }
    }

    @Transactional(readOnly = true)
    public List<PreviewSummaryView> listPreviews(UUID templateId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        return previewRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId).stream()
                .limit(PREVIEW_HISTORY_LIMIT)
                .map(this::toSummaryView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PreviewRecordView getPreview(UUID templateId, UUID previewId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        PreviewRecordEntity preview = previewRecordRepository.findById(previewId)
                .orElseThrow(PreviewNotFoundException::new);
        if (!preview.getTemplateId().equals(templateId)) {
            throw new PreviewNotFoundException();
        }
        List<AnchorBindingEntity> bindings = anchorBindingRepository
                .findByTemplateVersionIdOrderByAnchorIdAsc(preview.getTemplateVersionId());
        return toView(preview, readWarnings(preview.getFidelityWarningsJson()), bindings);
    }

    private PreviewRecordView toView(
            PreviewRecordEntity preview,
            List<FidelityWarningView> warnings,
            List<AnchorBindingEntity> bindings
    ) {
        var comparison = previewComparisonService.compare(bindings, warnings);
        return new PreviewRecordView(
                preview.getId().toString(),
                preview.getTemplateId().toString(),
                preview.getTemplateVersionId().toString(),
                preview.getStatus(),
                preview.getOutputFormat(),
                preview.getRenderProfileVersion(),
                preview.getArtifactStorageKey(),
                preview.getPdfArtifactStorageKey(),
                warnings,
                comparison,
                preview.getTestDataSetExternalId(),
                preview.getCreatedAt()
        );
    }

    private PreviewSummaryView toSummaryView(PreviewRecordEntity preview) {
        List<FidelityWarningView> warnings = readWarnings(preview.getFidelityWarningsJson());
        var comparison = previewComparisonService.compare(
                anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(preview.getTemplateVersionId()),
                warnings
        );
        return new PreviewSummaryView(
                preview.getId().toString(),
                preview.getTemplateVersionId().toString(),
                preview.getStatus(),
                preview.getTestDataSetExternalId(),
                preview.getCreatedAt(),
                preview.getCreatedBy(),
                warnings.size(),
                comparison.blockerCount(),
                comparison.warningCount(),
                preview.getStatus() == PreviewStatus.SUCCEEDED
                        && preview.getArtifactStorageKey() != null
                        && !preview.getArtifactStorageKey().isBlank(),
                preview.getStatus() == PreviewStatus.SUCCEEDED
                        && ((preview.getPdfArtifactStorageKey() != null
                                && !preview.getPdfArtifactStorageKey().isBlank())
                        || (preview.getArtifactStorageKey() != null
                                && !preview.getArtifactStorageKey().isBlank()))
        );
    }

    private Map<String, Object> resolveVariables(
            UUID templateId,
            TestGenerateRequest request,
            ManagementSessionClaims session
    ) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        if (request.testDataSetId() != null && !request.testDataSetId().isBlank()) {
            resolved.putAll(testDataSetService.resolveVariables(templateId, request.testDataSetId(), session));
        }
        if (request.variables() != null) {
            resolved.putAll(request.variables());
        }
        return resolved;
    }

    private String hashVariables(Map<String, Object> variables) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(variables);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception ex) {
            return "unknown";
        }
    }

    private String writeWarnings(List<FidelityWarningView> warnings) {
        try {
            return objectMapper.writeValueAsString(warnings);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }

    private List<FidelityWarningView> readWarnings(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return List.of();
            }
            List<FidelityWarningView> warnings = new java.util.ArrayList<>();
            for (com.fasterxml.jackson.databind.JsonNode node : root) {
                warnings.add(new FidelityWarningView(
                        node.path("code").asText(""),
                        node.path("messageKey").asText(""),
                        textOrNull(node, "location"),
                        textOrNull(node, "artifact"),
                        Boolean.valueOf(node.path("viewed").asBoolean(false))
                ));
            }
            return List.copyOf(warnings);
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private static String textOrNull(com.fasterxml.jackson.databind.JsonNode node, String field) {
        com.fasterxml.jackson.databind.JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }
}
