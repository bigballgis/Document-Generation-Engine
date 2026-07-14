package com.bank.docgen.rendering.service;

import com.bank.docgen.rendering.api.FidelityWarningView;
import com.bank.docgen.rendering.api.PreviewRecordView;
import com.bank.docgen.rendering.api.PreviewSummaryView;
import com.bank.docgen.rendering.api.TestGenerateRequest;
import com.bank.docgen.rendering.domain.PreviewStatus;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.port.TestDataSetEvidencePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Package-private preview record mapping, warning serialization, and variable helpers.
 */
final class PreviewRecordMappingSupport {

    private final PreviewComparisonService previewComparisonService;
    private final AnchorBindingRepository anchorBindingRepository;
    private final TestDataSetEvidencePort testDataSetEvidencePort;
    private final FidelityWarningJsonSupport fidelityWarningJsonSupport;
    private final ObjectMapper objectMapper;

    PreviewRecordMappingSupport(
            PreviewComparisonService previewComparisonService,
            AnchorBindingRepository anchorBindingRepository,
            TestDataSetEvidencePort testDataSetEvidencePort,
            FidelityWarningJsonSupport fidelityWarningJsonSupport,
            ObjectMapper objectMapper
    ) {
        this.previewComparisonService = previewComparisonService;
        this.anchorBindingRepository = anchorBindingRepository;
        this.testDataSetEvidencePort = testDataSetEvidencePort;
        this.fidelityWarningJsonSupport = fidelityWarningJsonSupport;
        this.objectMapper = objectMapper;
    }

    PreviewRecordView toView(
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

    PreviewSummaryView toSummaryView(PreviewRecordEntity preview) {
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

    Map<String, Object> resolveVariables(
            UUID templateId,
            TestGenerateRequest request,
            ManagementSessionClaims session
    ) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        if (request.testDataSetId() != null && !request.testDataSetId().isBlank()) {
            resolved.putAll(testDataSetEvidencePort.resolveVariables(templateId, request.testDataSetId(), session));
        }
        if (request.variables() != null) {
            resolved.putAll(request.variables());
        }
        return resolved;
    }

    String hashVariables(Map<String, Object> variables) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(variables);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception ex) {
            return "unknown";
        }
    }

    String writeWarnings(List<FidelityWarningView> warnings) {
        return fidelityWarningJsonSupport.writeWarnings(warnings);
    }

    List<FidelityWarningView> readWarnings(String json) {
        return fidelityWarningJsonSupport.readWarnings(json);
    }
}
