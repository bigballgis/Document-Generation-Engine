package com.bank.docgen.rendering.service;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.api.FidelityWarningView;
import com.bank.docgen.rendering.api.PreviewRecordView;
import com.bank.docgen.rendering.api.TestGenerateRequest;
import com.bank.docgen.rendering.domain.FidelityWarningCode;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.TemplateService;
import com.bank.docgen.template.service.TestDataSetService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreviewGenerationService {

    private final TemplateService templateService;
    private final TestDataSetService testDataSetService;
    private final TemplateVersionRepository templateVersionRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final PreviewRecordRepository previewRecordRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocxAssembler docxAssembler;
    private final ObjectMapper objectMapper;

    public PreviewGenerationService(
            TemplateService templateService,
            TestDataSetService testDataSetService,
            TemplateVersionRepository templateVersionRepository,
            AnchorBindingRepository anchorBindingRepository,
            MasterDocumentRepository masterDocumentRepository,
            PreviewRecordRepository previewRecordRepository,
            ObjectStoragePort objectStoragePort,
            DocxAssembler docxAssembler,
            ObjectMapper objectMapper
    ) {
        this.templateService = templateService;
        this.testDataSetService = testDataSetService;
        this.templateVersionRepository = templateVersionRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.previewRecordRepository = previewRecordRepository;
        this.objectStoragePort = objectStoragePort;
        this.docxAssembler = docxAssembler;
        this.objectMapper = objectMapper;
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
        TemplateVersionEntity version = templateVersionRepository.findByTemplateIdAndDevVersionNumber(templateId, 1)
                .orElseThrow(TemplateNotFoundException::new);
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
        previewRecordRepository.save(preview);
        try {
            MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(template.getMasterId())
                    .orElseThrow(MasterNotFoundException::new);
            List<AnchorBindingEntity> bindings = anchorBindingRepository
                    .findByTemplateVersionIdOrderByAnchorIdAsc(version.getId());
            Map<String, String> bindingJson = new LinkedHashMap<>();
            bindings.forEach(binding -> bindingJson.put(binding.getAnchorId(), binding.getStructuredContentJson()));
            Map<String, String> anchorContent = docxAssembler.buildAnchorReplacements(bindingJson, variables);
            byte[] docx;
            try (InputStream masterStream = objectStoragePort.get(master.getStorageKey())) {
                docx = docxAssembler.assemble(masterStream, anchorContent);
            }
            String storageKey = "previews/" + preview.getId() + "/output.docx";
            objectStoragePort.put(
                    storageKey,
                    new java.io.ByteArrayInputStream(docx),
                    docx.length,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );
            List<FidelityWarningView> warnings = List.of(
                    new FidelityWarningView(
                            FidelityWarningCode.CONTROLLED_STYLE_FALLBACK.name(),
                            "generation.warning.fidelity.controlledStyleFallback"
                    )
            );
            preview.markSucceeded(storageKey, writeWarnings(warnings));
            previewRecordRepository.save(preview);
            if (request.testDataSetId() != null && !request.testDataSetId().isBlank()) {
                testDataSetService.lockForEvidence(templateId, request.testDataSetId());
            }
            return toView(preview, warnings, bindings.size());
        } catch (Exception ex) {
            preview.markFailed();
            previewRecordRepository.save(preview);
            if (throwOnFailure) {
                throw new PreviewGenerationException("api.error.rendering.generationFailed", ex);
            }
            return toView(preview, List.of(), 0);
        }
    }

    @Transactional(readOnly = true)
    public PreviewRecordView getPreview(UUID templateId, UUID previewId, ManagementSessionClaims session) {
        templateService.requireReadableTemplate(templateId, session);
        PreviewRecordEntity preview = previewRecordRepository.findById(previewId)
                .orElseThrow(PreviewNotFoundException::new);
        if (!preview.getTemplateId().equals(templateId)) {
            throw new PreviewNotFoundException();
        }
        int bindingCount = anchorBindingRepository
                .findByTemplateVersionIdOrderByAnchorIdAsc(preview.getTemplateVersionId())
                .size();
        return toView(preview, readWarnings(preview.getFidelityWarningsJson()), bindingCount);
    }

    private PreviewRecordView toView(PreviewRecordEntity preview, List<FidelityWarningView> warnings, int bindingCount) {
        String comparisonSummary = "anchorsConfigured=" + bindingCount + ";warnings=" + warnings.size();
        return new PreviewRecordView(
                preview.getId().toString(),
                preview.getTemplateId().toString(),
                preview.getTemplateVersionId().toString(),
                preview.getStatus(),
                preview.getOutputFormat(),
                preview.getArtifactStorageKey(),
                warnings,
                comparisonSummary,
                preview.getTestDataSetExternalId(),
                preview.getCreatedAt()
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
            return objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, FidelityWarningView.class)
            );
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }
}
