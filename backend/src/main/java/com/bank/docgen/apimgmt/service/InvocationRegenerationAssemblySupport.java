package com.bank.docgen.apimgmt.service;

import com.bank.docgen.authoring.structured.CallerRenderOverride;
import com.bank.docgen.authoring.structured.RenderProfileService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.rendering.DocxAssembler;
import com.bank.docgen.rendering.DocxAssemblyException;
import com.bank.docgen.rendering.DocxSpecimenWatermarkStamper;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.PdfSpecimenWatermarkStamper;
import com.bank.docgen.rendering.RenderingOperationException;
import com.bank.docgen.runtime.persistence.ApiInvocationRecordEntity;
import com.bank.docgen.runtime.service.InvocationRetentionVariableRedactor;
import com.bank.docgen.sharedkernel.api.ApiErrorCategories;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.document.RenderProfile;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateContentModuleReferenceService;
import com.bank.docgen.template.service.VariableComputeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * CE-G06 controlled regenerate assembly — pinned master + SPECIMEN watermark (reuse G02).
 * Formal runtime generation path is untouched.
 */
@Component
public class InvocationRegenerationAssemblySupport {

    private static final EncryptionOptionsView NO_ENCRYPTION =
            new EncryptionOptionsView(false, null, null, null);
    private static final String PINNED_MASTER_UNAVAILABLE = "api.error.rendering.pinnedMasterUnavailable";

    private final TemplateVersionRepository templateVersionRepository;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final AnchorBindingRepository anchorBindingRepository;
    private final ObjectStoragePort objectStoragePort;
    private final DocxAssembler docxAssembler;
    private final DocumentArtifactPipeline documentArtifactPipeline;
    private final TemplateContentModuleReferenceService contentModuleReferenceService;
    private final RenderProfileService renderProfileService;
    private final VariableComputeService variableComputeService;
    private final ObjectMapper objectMapper;

    public InvocationRegenerationAssemblySupport(
            TemplateVersionRepository templateVersionRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            AnchorBindingRepository anchorBindingRepository,
            ObjectStoragePort objectStoragePort,
            DocxAssembler docxAssembler,
            DocumentArtifactPipeline documentArtifactPipeline,
            TemplateContentModuleReferenceService contentModuleReferenceService,
            RenderProfileService renderProfileService,
            VariableComputeService variableComputeService,
            ObjectMapper objectMapper
    ) {
        this.templateVersionRepository = templateVersionRepository;
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.anchorBindingRepository = anchorBindingRepository;
        this.objectStoragePort = objectStoragePort;
        this.docxAssembler = docxAssembler;
        this.documentArtifactPipeline = documentArtifactPipeline;
        this.contentModuleReferenceService = contentModuleReferenceService;
        this.renderProfileService = renderProfileService;
        this.variableComputeService = variableComputeService;
        this.objectMapper = objectMapper;
    }

    public record AssembledRegeneration(String artifactStorageKey, String contentType) {
    }

    public AssembledRegeneration assembleSpecimen(
            TemplateEntity template,
            ApiInvocationRecordEntity invocation,
            String outputFormat,
            UUID regenerationId
    ) {
        TemplateVersionEntity version = templateVersionRepository
                .findById(invocation.getReleaseBundleSnapshotId())
                .orElseThrow(this::pinnedMasterUnavailable);
        if (!version.getTemplateId().equals(template.getId())) {
            throw pinnedMasterUnavailable();
        }
        MasterRevisionLineEntity revision = resolvePinnedRevision(template, version);
        byte[] masterBytes = readMasterBytes(revision.getStorageKey());
        assertBundleHashMatches(masterBytes, invocation.getReleaseBundleHash());

        Map<String, Object> variables = extractVariables(invocation.getParametersStorage());
        // IBL-A6: replay retained contextSummary.locale (null when absent/blank → engine default).
        String localeTag = extractLocaleTag(invocation.getParametersStorage());
        Map<String, Object> resolvedVariables = variableComputeService.applyCompute(
                version.getId(),
                variables,
                localeTag
        );
        List<AnchorBindingEntity> bindings = anchorBindingRepository
                .findByTemplateVersionIdOrderByAnchorIdAsc(version.getId());
        Map<String, String> bindingJson = new LinkedHashMap<>();
        bindings.forEach(binding -> bindingJson.put(binding.getAnchorId(), binding.getStructuredContentJson()));
        Map<String, String> pinnedModuleStructures =
                contentModuleReferenceService.resolvePinnedContentStructures(version.getId());

        byte[] docx;
        try (InputStream masterStream = new ByteArrayInputStream(masterBytes)) {
            docx = docxAssembler.assembleStructured(
                    masterStream,
                    bindingJson,
                    resolvedVariables,
                    pinnedModuleStructures
            );
        } catch (DocxAssemblyException | RenderingOperationException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new RenderingOperationException("api.error.rendering.generationFailed");
        }

        try {
            docx = DocxSpecimenWatermarkStamper.apply(docx);
        } catch (RuntimeException ex) {
            throw specimenWatermarkFailed();
        }

        String format = outputFormat == null || outputFormat.isBlank()
                ? "PDF"
                : outputFormat.trim().toUpperCase(Locale.ROOT);
        RenderProfile renderProfile = renderProfileService.resolveEffectiveProfile(
                version,
                CallerRenderOverride.empty()
        );
        if ("DOCX".equals(format)) {
            String storageKey = "regenerations/" + regenerationId + "/output.docx";
            objectStoragePort.put(
                    storageKey,
                    new ByteArrayInputStream(docx),
                    docx.length,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );
            return new AssembledRegeneration(
                    storageKey,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            );
        }

        DocumentArtifactPipeline.GeneratedArtifact pdfArtifact = documentArtifactPipeline.finalizeArtifact(
                docx,
                "PDF",
                NO_ENCRYPTION,
                renderProfile
        );
        try (pdfArtifact) {
            byte[] pdfBytes;
            try (InputStream pdfStream = pdfArtifact.spooled().openInputStream()) {
                pdfBytes = pdfStream.readAllBytes();
            }
            try {
                pdfBytes = PdfSpecimenWatermarkStamper.apply(pdfBytes);
            } catch (RuntimeException ex) {
                throw specimenWatermarkFailed();
            }
            String storageKey = "regenerations/" + regenerationId + "/output.pdf";
            objectStoragePort.put(
                    storageKey,
                    new ByteArrayInputStream(pdfBytes),
                    pdfBytes.length,
                    pdfArtifact.contentType()
            );
            return new AssembledRegeneration(storageKey, pdfArtifact.contentType());
        } catch (InvocationRegenerationException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new RenderingOperationException("api.error.rendering.generationFailed");
        }
    }

    private MasterRevisionLineEntity resolvePinnedRevision(
            TemplateEntity template,
            TemplateVersionEntity version
    ) {
        UUID pinnedRevisionId = version.getMasterRevisionId();
        if (pinnedRevisionId == null) {
            throw pinnedMasterUnavailable();
        }
        return masterRevisionLineRepository
                .findByIdAndMasterIdAndDeletedAtIsNull(pinnedRevisionId, template.getMasterId())
                .orElseThrow(this::pinnedMasterUnavailable);
    }

    private byte[] readMasterBytes(String storageKey) {
        try (InputStream stream = objectStoragePort.get(storageKey)) {
            return stream.readAllBytes();
        } catch (IOException | RuntimeException ex) {
            throw pinnedMasterUnavailable();
        }
    }

    private void assertBundleHashMatches(byte[] masterBytes, String expectedHash) {
        String actual = sha256Hex(masterBytes);
        if (expectedHash == null || !expectedHash.equalsIgnoreCase(actual)) {
            throw new InvocationRegenerationException(
                    HttpStatus.CONFLICT,
                    ApiErrorCodes.RELEASE_BUNDLE_HASH_MISMATCH,
                    ApiErrorCategories.GENERATION,
                    "api.error.audit.releaseBundleHashMismatch"
            );
        }
    }

    Map<String, Object> extractVariables(String parametersStorage) {
        try {
            JsonNode root = objectMapper.readTree(parametersStorage == null ? "{}" : parametersStorage);
            if (root.hasNonNull("variables") && root.get("variables").isObject()) {
                return replayVariables(root.get("variables"));
            }
            JsonNode items = root.get("items");
            if (items != null && items.isArray() && items.size() == 1
                    && items.get(0).hasNonNull("variables")
                    && items.get(0).get("variables").isObject()) {
                return replayVariables(items.get(0).get("variables"));
            }
        } catch (IOException | RuntimeException ignored) {
            // fall through to fail-closed
        }
        throw new InvocationRegenerationException(
                HttpStatus.CONFLICT,
                ApiErrorCodes.RELEASE_BUNDLE_SNAPSHOT_UNAVAILABLE,
                ApiErrorCategories.GENERATION,
                "api.error.audit.releaseBundleSnapshotUnavailable"
        );
    }

    /**
     * IBL-A6 / A6-C1: locale for compute replay from retained {@code contextSummary.locale}.
     * Blank or missing → {@code null} (engine default; A6-C4 / A6-C6).
     */
    String extractLocaleTag(String parametersStorage) {
        try {
            JsonNode root = objectMapper.readTree(parametersStorage == null ? "{}" : parametersStorage);
            JsonNode localeNode = root.path("contextSummary").path("locale");
            if (localeNode.isMissingNode() || localeNode.isNull() || !localeNode.isTextual()) {
                return null;
            }
            String locale = localeNode.asText();
            if (locale == null || locale.isBlank()) {
                return null;
            }
            return locale.trim();
        } catch (IOException | RuntimeException ignored) {
            return null;
        }
    }

    private Map<String, Object> replayVariables(JsonNode variablesNode) {
        Map<String, Object> retained = objectMapper.convertValue(
                variablesNode,
                new TypeReference<LinkedHashMap<String, Object>>() {
                }
        );
        // IBL-A5: drop redaction sentinels / nulls so regenerate does not require PII cleartext.
        return new LinkedHashMap<>(InvocationRetentionVariableRedactor.toReplayVariables(retained));
    }

    private InvocationRegenerationException pinnedMasterUnavailable() {
        return new InvocationRegenerationException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ApiErrorCodes.PINNED_MASTER_UNAVAILABLE,
                ApiErrorCategories.RENDERING,
                PINNED_MASTER_UNAVAILABLE
        );
    }

    private InvocationRegenerationException specimenWatermarkFailed() {
        return new InvocationRegenerationException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ApiErrorCodes.SPECIMEN_WATERMARK_FAILED,
                ApiErrorCategories.GENERATION,
                "api.error.audit.specimenWatermarkFailed"
        );
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
