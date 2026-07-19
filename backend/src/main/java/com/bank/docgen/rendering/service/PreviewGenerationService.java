package com.bank.docgen.rendering.service;

import com.bank.docgen.authoring.structured.FidelityValidationService;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.PaginationDeltaFidelitySupport;
import com.bank.docgen.rendering.api.PreviewCompositionContext;
import com.bank.docgen.rendering.api.PreviewRecordView;
import com.bank.docgen.rendering.api.PreviewSummaryView;
import com.bank.docgen.rendering.api.TestGenerateRequest;
import com.bank.docgen.rendering.persistence.PreviewRecordEntity;
import com.bank.docgen.rendering.persistence.PreviewRecordRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.port.RenderableTemplateSnapshot;
import com.bank.docgen.template.port.TemplatePreviewAuthorizationPort;
import com.bank.docgen.template.port.TemplateRenderContextPort;
import com.bank.docgen.template.port.TestDataSetEvidencePort;
import com.bank.docgen.template.port.VariableComputePort;
import com.bank.docgen.template.port.VariableSchemaValidationPort;
import com.bank.docgen.template.port.CompositionInclusionAxes;
import com.bank.docgen.template.port.CompositionInclusionUnsatisfiedException;
import com.bank.docgen.template.port.ContentModuleJurisdictionMismatchException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreviewGenerationService {

    private static final Logger LOG = LoggerFactory.getLogger(PreviewGenerationService.class);
    private static final int PREVIEW_HISTORY_LIMIT = 50;

    private final TemplatePreviewAuthorizationPort previewAuthorizationPort;
    private final TestDataSetEvidencePort testDataSetEvidencePort;
    private final TemplateRenderContextPort renderContextPort;
    private final AnchorBindingRepository anchorBindingRepository;
    private final PreviewRecordRepository previewRecordRepository;
    private final RenderProfileService renderProfileService;
    private final PreviewRecordMappingSupport mapping;
    private final PreviewGenerationAssemblySupport assembly;

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
            FidelityValidationService fidelityValidationService,
            FidelityWarningJsonSupport fidelityWarningJsonSupport,
            VariableComputePort variableComputePort,
            VariableSchemaValidationPort variableSchemaValidationPort,
            PaginationDeltaFidelitySupport paginationDeltaFidelitySupport
    ) {
        this.previewAuthorizationPort = previewAuthorizationPort;
        this.testDataSetEvidencePort = testDataSetEvidencePort;
        this.renderContextPort = renderContextPort;
        this.anchorBindingRepository = anchorBindingRepository;
        this.previewRecordRepository = previewRecordRepository;
        this.renderProfileService = renderProfileService;
        this.mapping = new PreviewRecordMappingSupport(
                previewComparisonService,
                anchorBindingRepository,
                testDataSetEvidencePort,
                fidelityWarningJsonSupport,
                objectMapper
        );
        this.assembly = new PreviewGenerationAssemblySupport(
                anchorBindingRepository,
                masterDocumentRepository,
                objectStoragePort,
                docxAssembler,
                documentArtifactPipeline,
                renderContextPort,
                renderProfileService,
                fidelityValidationService,
                variableComputePort,
                variableSchemaValidationPort,
                paginationDeltaFidelitySupport
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
        CompositionInclusionAxes inclusionAxes = inclusionAxesFrom(request.context());
        try {
            PreviewGenerationAssemblySupport.AssembledPreview assembled =
                    assembly.assembleAndStore(
                            template,
                            version,
                            preview.getId(),
                            variables,
                            null,
                            inclusionAxes
                    );
            preview.markSucceeded(
                    assembled.storageKey(),
                    assembled.pdfStorageKey(),
                    mapping.writeWarnings(assembled.warnings()),
                    assembled.pdfPageCount()
            );
            previewRecordRepository.save(preview);
            if (request.testDataSetId() != null && !request.testDataSetId().isBlank()) {
                testDataSetEvidencePort.lockForEvidence(templateId, request.testDataSetId());
            }
            return mapping.toView(preview, assembled.warnings(), assembled.bindings());
        } catch (com.bank.docgen.sharedkernel.document.variable.VariableValidationException validationEx) {
            LOG.warn("Preview variable validation failed for template {} preview {}: {}",
                    templateId, preview.getId(), validationEx.getMessage());
            preview.markFailed();
            previewRecordRepository.save(preview);
            if (throwOnFailure) {
                throw validationEx;
            }
            return mapping.toView(preview, List.of(), List.of());
        } catch (com.bank.docgen.sharedkernel.document.compute.VariableComputeException computeEx) {
            LOG.warn("Preview compute failed for template {} preview {}: {}", templateId, preview.getId(), computeEx.getMessage());
            preview.markFailed();
            previewRecordRepository.save(preview);
            if (throwOnFailure) {
                throw computeEx;
            }
            return mapping.toView(preview, List.of(), List.of());
        } catch (CompositionInclusionUnsatisfiedException | ContentModuleJurisdictionMismatchException inclusionEx) {
            LOG.warn("Preview composition inclusion failed for template {} preview {}: {}",
                    templateId, preview.getId(), inclusionEx.getMessage());
            preview.markFailed();
            previewRecordRepository.save(preview);
            if (throwOnFailure) {
                throw inclusionEx;
            }
            return mapping.toView(preview, List.of(), List.of());
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

    private static CompositionInclusionAxes inclusionAxesFrom(PreviewCompositionContext context) {
        if (context == null) {
            return CompositionInclusionAxes.empty();
        }
        return CompositionInclusionAxes.of(context.jurisdiction(), context.product(), context.channel());
    }

    @Transactional(readOnly = true)
    public List<PreviewSummaryView> listPreviews(UUID templateId, ManagementSessionClaims session) {
        previewAuthorizationPort.requireReadableSnapshot(templateId, session);
        // PRR-A02: TopN at DB — do not load-all then stream().limit.
        return previewRecordRepository
                .findByTemplateIdOrderByCreatedAtDesc(templateId, PageRequest.of(0, PREVIEW_HISTORY_LIMIT))
                .stream()
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
