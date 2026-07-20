package com.bank.docgen.runtime.service;

import com.bank.docgen.authoring.structured.CallerRenderOverride;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.documentbrand.domain.ResolvedDocumentBrand;
import com.bank.docgen.documentbrand.service.AllowedDocumentBrandCodesJsonSupport;
import com.bank.docgen.documentbrand.service.DocumentBrandResolveService;
import com.bank.docgen.documentbrand.service.DocumentBrandSlotApplicationSupport;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocxAssemblyException;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.PaginationDeltaFidelitySupport;
import com.bank.docgen.rendering.RenderingOperationException;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.document.RenderProfile;
import com.bank.docgen.sharedkernel.document.fidelity.PaginationDeltaEvaluator;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.port.VariableSchemaValidationPort;
import com.bank.docgen.template.service.CompositionInclusionAssemblySupport;
import com.bank.docgen.template.port.CompositionInclusionAxes;
import com.bank.docgen.template.service.CompositionInclusionRuleService;
import com.bank.docgen.template.service.TemplateContentModuleReferenceService;
import com.bank.docgen.template.service.TemplateNotFoundException;
import com.bank.docgen.template.service.VariableComputeService;
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
    private static final String PINNED_MASTER_UNAVAILABLE = "api.error.rendering.pinnedMasterUnavailable";

    private final TemplateVersionRepository templateVersionRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocxAssembler docxAssembler;
    private final DocumentArtifactPipeline documentArtifactPipeline;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final CompositionInclusionRuleService compositionInclusionRuleService;
    private final RenderProfileService renderProfileService;
    private final VersionFidelityWarningService versionFidelityWarningService;
    private final VariableComputeService variableComputeService;
    private final VariableSchemaValidationPort variableSchemaValidationPort;
    private final PaginationDeltaFidelitySupport paginationDeltaFidelitySupport;
    private final DocumentBrandResolveService documentBrandResolveService;

    DocumentGenerationAssemblySupport(
            TemplateVersionRepository templateVersionRepository,
            AnchorBindingRepository anchorBindingRepository,
            MasterDocumentRepository masterDocumentRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            ObjectStoragePort objectStoragePort,
            DocxAssembler docxAssembler,
            DocumentArtifactPipeline documentArtifactPipeline,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            CompositionInclusionRuleService compositionInclusionRuleService,
            RenderProfileService renderProfileService,
            VersionFidelityWarningService versionFidelityWarningService,
            VariableComputeService variableComputeService,
            VariableSchemaValidationPort variableSchemaValidationPort,
            PaginationDeltaFidelitySupport paginationDeltaFidelitySupport,
            DocumentBrandResolveService documentBrandResolveService
    ) {
        this.templateVersionRepository = templateVersionRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.objectStoragePort = objectStoragePort;
        this.docxAssembler = docxAssembler;
        this.documentArtifactPipeline = documentArtifactPipeline;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.compositionInclusionRuleService = compositionInclusionRuleService;
        this.renderProfileService = renderProfileService;
        this.versionFidelityWarningService = versionFidelityWarningService;
        this.variableComputeService = variableComputeService;
        this.variableSchemaValidationPort = variableSchemaValidationPort;
        this.paginationDeltaFidelitySupport = paginationDeltaFidelitySupport;
        this.documentBrandResolveService = documentBrandResolveService;
    }

    DocumentGenerationEngine.GeneratedDocument generate(
            TemplateEntity template,
            String releaseVersion,
            Map<String, Object> variables,
            String outputFormat,
            EncryptionOptionsView encryption,
            CallerRenderOverride callerRenderOverride
    ) {
        return generate(
                template,
                releaseVersion,
                variables,
                outputFormat,
                encryption,
                callerRenderOverride,
                null,
                CompositionInclusionAxes.empty()
        );
    }

    DocumentGenerationEngine.GeneratedDocument generate(
            TemplateEntity template,
            String releaseVersion,
            Map<String, Object> variables,
            String outputFormat,
            EncryptionOptionsView encryption,
            CallerRenderOverride callerRenderOverride,
            String localeTag
    ) {
        return generate(
                template,
                releaseVersion,
                variables,
                outputFormat,
                encryption,
                callerRenderOverride,
                localeTag,
                CompositionInclusionAxes.empty()
        );
    }

    DocumentGenerationEngine.GeneratedDocument generate(
            TemplateEntity template,
            String releaseVersion,
            Map<String, Object> variables,
            String outputFormat,
            EncryptionOptionsView encryption,
            CallerRenderOverride callerRenderOverride,
            String localeTag,
            CompositionInclusionAxes inclusionAxes
    ) {
        return generate(
                template,
                releaseVersion,
                variables,
                outputFormat,
                encryption,
                callerRenderOverride,
                localeTag,
                inclusionAxes,
                null
        );
    }

    DocumentGenerationEngine.GeneratedDocument generate(
            TemplateEntity template,
            String releaseVersion,
            Map<String, Object> variables,
            String outputFormat,
            EncryptionOptionsView encryption,
            CallerRenderOverride callerRenderOverride,
            String localeTag,
            CompositionInclusionAxes inclusionAxes,
            String legalEntityCode
    ) {
        TemplateVersionEntity version = templateVersionRepository
                .findByTemplateIdAndReleaseVersion(template.getId(), releaseVersion)
                .orElseThrow(TemplateNotFoundException::new);
        // IBL-A1: fail-closed VariableSchema validation before compute/assemble (no silent blank).
        variableSchemaValidationPort.validateForAssembly(version.getId(), variables);
        // CE-K03: evaluate compute expressions before DOCX assembly (fail-closed).
        Map<String, Object> resolvedVariables = variableComputeService.applyCompute(
                version.getId(),
                variables,
                localeTag
        );
        // IBL-E4 / ADR-0065: resolve document brand (fail-closed) then apply to brand slots.
        ResolvedDocumentBrand resolvedBrand = documentBrandResolveService.resolve(
                template.getGroupCode(),
                legalEntityCode,
                AllowedDocumentBrandCodesJsonSupport.parse(template.getAllowedDocumentBrandCodesJson())
        );
        RenderProfile renderProfile = renderProfileService.resolveEffectiveProfile(
                version,
                callerRenderOverride == null ? CallerRenderOverride.empty() : callerRenderOverride
        );
        String masterStorageKey = resolveMasterStorageKey(template, version);
        List<AnchorBindingEntity> bindings = anchorBindingRepository
                .findByTemplateVersionIdOrderByAnchorIdAsc(version.getId());
        Map<String, String> sourceBindings = new LinkedHashMap<>();
        bindings.forEach(binding -> sourceBindings.put(binding.getAnchorId(), binding.getStructuredContentJson()));
        DocumentBrandSlotApplicationSupport.Applied brandApplied =
                DocumentBrandSlotApplicationSupport.apply(sourceBindings, resolvedBrand);
        Map<String, String> bindingJson = new LinkedHashMap<>(brandApplied.bindingJson());
        Map<String, String> allPinned =
                contentModuleReferenceService.resolvePinnedContentStructures(version.getId());
        CompositionInclusionAssemblySupport.AppliedInclusion applied =
                CompositionInclusionAssemblySupport.apply(
                        version,
                        allPinned,
                        contentModuleReferenceService.resolvePinnedJurisdictions(version.getId()),
                        compositionInclusionRuleService.loadRules(version),
                        inclusionAxes == null ? CompositionInclusionAxes.empty() : inclusionAxes
                );
        Map<String, String> pinnedModuleStructures = applied.pinnedStructures();
        byte[] docx;
        try (InputStream masterStream = openMasterStream(masterStorageKey, template, version)) {
            docx = docxAssembler.assembleStructured(
                    masterStream,
                    bindingJson,
                    resolvedVariables,
                    pinnedModuleStructures
            );
        } catch (DocxAssemblyException | RenderingOperationException ex) {
            // Preserve CE-K01 pinnedMasterUnavailable (and assembly-domain failures) — do not
            // collapse them into a generic generationFailed.
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
            byte[] artifactBytes;
            try (InputStream artifactStream = artifact.spooled().openInputStream()) {
                artifactBytes = artifactStream.readAllBytes();
            }
            objectStoragePort.put(
                    storageKey,
                    new java.io.ByteArrayInputStream(artifactBytes),
                    artifactBytes.length,
                    artifact.contentType()
            );
            List<String> fidelityWarnings = new ArrayList<>(
                    versionFidelityWarningService.resolveWarningCodes(version, template.getMasterId())
            );
            fidelityWarnings.addAll(artifact.pipelineWarningCodes());
            fidelityWarnings.addAll(brandApplied.fidelityWarningCodes());
            if ("PDF".equalsIgnoreCase(outputFormat)) {
                Integer pdfPageCount = paginationDeltaFidelitySupport.measurePdfPages(artifactBytes);
                PaginationDeltaEvaluator.Evaluation paginationEval = paginationDeltaFidelitySupport.evaluate(
                        version.getAuthorWordPageCount(),
                        pdfPageCount
                );
                paginationDeltaFidelitySupport.warningCodeIfNeeded(paginationEval)
                        .ifPresent(fidelityWarnings::add);
            }
            return new DocumentGenerationEngine.GeneratedDocument(
                    documentId,
                    storageKey,
                    null,
                    artifact.contentType(),
                    outputFormat,
                    List.copyOf(fidelityWarnings),
                    resolvedBrand.legalEntityCode(),
                    resolvedBrand.documentBrandCode()
            );
        } catch (IOException ex) {
            throw new RenderingOperationException("api.error.rendering.generationFailed");
        }
    }

    /**
     * CE-K01: resolve the master DOCX storage key for assembly.
     *
     * <p>PUBLISHED versions read the immutable pinned revision's storage key
     * ({@code template_version.master_revision_id} → {@code master_revision_line.storage_key})
     * and fail closed ({@code api.error.rendering.pinnedMasterUnavailable}) when the pin is
     * missing or the revision/storage object is unavailable — never falling back to the live
     * master. Non-PUBLISHED versions (preview / dev) keep the legacy live-master path so the
     * preview semantics stay unchanged (BDD-CE-K01-020).
     */
    private String resolveMasterStorageKey(TemplateEntity template, TemplateVersionEntity version) {
        if (version.getLifecycleStatus() != TemplateLifecycleStatus.PUBLISHED) {
            MasterDocumentEntity master = masterDocumentRepository
                    .findByIdAndDeletedAtIsNull(template.getMasterId())
                    .orElseThrow(() -> {
                        LOG.warn("CE-K01 live master lookup failed for template {}", template.getId());
                        return new RenderingOperationException(PINNED_MASTER_UNAVAILABLE);
                    });
            return master.getStorageKey();
        }
        UUID pinnedRevisionId = version.getMasterRevisionId();
        if (pinnedRevisionId == null) {
            LOG.warn("CE-K01 published version {} has no pinned master_revision_id (template={})",
                    version.getId(), template.getId());
            throw new RenderingOperationException(PINNED_MASTER_UNAVAILABLE);
        }
        MasterRevisionLineEntity revision = masterRevisionLineRepository
                .findByIdAndMasterIdAndDeletedAtIsNull(pinnedRevisionId, template.getMasterId())
                .orElseThrow(() -> {
                    LOG.warn("CE-K01 pinned revision {} not found for template {} (version {})",
                            pinnedRevisionId, template.getId(), version.getId());
                    return new RenderingOperationException(PINNED_MASTER_UNAVAILABLE);
                });
        return revision.getStorageKey();
    }

    /**
     * Open the master DOCX stream, converting storage-read failures (e.g. object-storage 404
     * on the pinned revision) into {@code api.error.rendering.pinnedMasterUnavailable} so the
     * assembly never silently falls back or reports a generic generation failure.
     */
    private InputStream openMasterStream(String storageKey, TemplateEntity template, TemplateVersionEntity version) {
        try {
            return objectStoragePort.get(storageKey);
        } catch (RuntimeException ex) {
            LOG.warn("CE-K01 master storage read failed for key {} (template={}, version={}): {}",
                    storageKey, template.getId(), version.getId(), ex.getMessage());
            throw new RenderingOperationException(PINNED_MASTER_UNAVAILABLE);
        }
    }
}
