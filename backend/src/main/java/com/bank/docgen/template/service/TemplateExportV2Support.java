package com.bank.docgen.template.service;

import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.TemplateExportAssetKeyManifestItemView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.api.TemplateExportRenderProfileView;
import com.bank.docgen.template.domain.TemplateExportAssetKeyUsage;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * CE-E01: assembles v2 self-contained export fields and pinned master DOCX bytes.
 */
@Component
public class TemplateExportV2Support {

    public static final String EXPORT_FORMAT_V2 = "template-export-bundle-v2-json";
    public static final String ZIP_MASTER_ENTRY = "artifacts/master.docx";
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final ObjectStoragePort objectStoragePort;
    private final ContentModuleRepository contentModuleRepository;
    private final ContentModuleVersionRepository contentModuleVersionRepository;
    private final ObjectMapper objectMapper;

    public TemplateExportV2Support(
            MasterDocumentRepository masterDocumentRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            ObjectStoragePort objectStoragePort,
            ContentModuleRepository contentModuleRepository,
            ContentModuleVersionRepository contentModuleVersionRepository,
            ObjectMapper objectMapper
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.objectStoragePort = objectStoragePort;
        this.contentModuleRepository = contentModuleRepository;
        this.contentModuleVersionRepository = contentModuleVersionRepository;
        this.objectMapper = objectMapper;
    }

    public record V2Artifacts(
            TemplateExportMasterPinView masterPin,
            List<TemplateExportClauseSnapshotView> clauseSnapshots,
            TemplateExportRenderProfileView renderProfile,
            List<TemplateExportAssetKeyManifestItemView> assetKeyManifest,
            byte[] masterDocxBytes
    ) {
        public V2Artifacts {
            clauseSnapshots = clauseSnapshots == null ? List.of() : List.copyOf(clauseSnapshots);
            assetKeyManifest = assetKeyManifest == null ? List.of() : List.copyOf(assetKeyManifest);
            masterDocxBytes = masterDocxBytes == null
                    ? new byte[0]
                    : com.bank.docgen.sharedkernel.api.DefensiveCopies.copyBytes(masterDocxBytes);
        }
    }

    public V2Artifacts assemble(
            TemplateEntity template,
            TemplateVersionEntity version,
            List<ContentModuleReferenceView> references,
            List<AnchorBindingView> bindings
    ) {
        ResolvedMaster resolved = resolveMasterPinAndBytes(template, version);
        List<TemplateExportClauseSnapshotView> clauses = snapshotClauses(references);
        TemplateExportRenderProfileView profile = resolveRenderProfile(version);
        List<TemplateExportAssetKeyManifestItemView> assets = collectAssetKeys(bindings, clauses);
        return new V2Artifacts(resolved.pin(), clauses, profile, assets, resolved.bytes());
    }

    private ResolvedMaster resolveMasterPinAndBytes(TemplateEntity template, TemplateVersionEntity version) {
        UUID masterId = template.getMasterId();
        if (version.getMasterRevisionId() != null && version.getMasterFileHash() != null
                && !version.getMasterFileHash().isBlank()) {
            MasterRevisionLineEntity revision = masterRevisionLineRepository
                    .findByIdAndMasterIdAndDeletedAtIsNull(version.getMasterRevisionId(), masterId)
                    .orElseThrow(this::pinnedMasterUnavailable);
            byte[] bytes = readMasterBytes(revision.getStorageKey());
            String hash = TemplateExportHashSupport.sha256Hex(bytes);
            if (!hash.equalsIgnoreCase(version.getMasterFileHash())) {
                throw pinnedMasterUnavailable();
            }
            String pinOrigin = parsePinOrigin(version.getPinMetadataJson());
            return new ResolvedMaster(
                    new TemplateExportMasterPinView(
                            revision.getId().toString(),
                            hash.toLowerCase(Locale.ROOT),
                            revision.getRevisionSequence(),
                            pinOrigin
                    ),
                    bytes
            );
        }
        // EXPORT_TIME: pending release without pin — hash current revision without DB mutation
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)
                .orElseThrow(this::pinnedMasterUnavailable);
        UUID revisionId = master.getCurrentRevisionLineId();
        if (revisionId == null) {
            throw pinnedMasterUnavailable();
        }
        MasterRevisionLineEntity revision = masterRevisionLineRepository
                .findByIdAndMasterIdAndDeletedAtIsNull(revisionId, masterId)
                .orElseThrow(this::pinnedMasterUnavailable);
        byte[] bytes = readMasterBytes(revision.getStorageKey());
        String hash = TemplateExportHashSupport.sha256Hex(bytes);
        return new ResolvedMaster(
                new TemplateExportMasterPinView(
                        revision.getId().toString(),
                        hash,
                        revision.getRevisionSequence(),
                        "EXPORT_TIME"
                ),
                bytes
        );
    }

    private byte[] readMasterBytes(String storageKey) {
        try (InputStream stream = objectStoragePort.get(storageKey)) {
            return stream.readAllBytes();
        } catch (IOException | RuntimeException ex) {
            throw pinnedMasterUnavailable();
        }
    }

    private TemplateGovernanceException pinnedMasterUnavailable() {
        return new TemplateGovernanceException(
                ApiErrorCodes.PINNED_MASTER_UNAVAILABLE,
                "api.error.rendering.pinnedMasterUnavailable",
                HttpStatus.UNPROCESSABLE_ENTITY
        );
    }

    private String parsePinOrigin(String pinMetadataJson) {
        if (pinMetadataJson == null || pinMetadataJson.isBlank()) {
            return "PUBLISHED";
        }
        try {
            JsonNode node = objectMapper.readTree(pinMetadataJson);
            String origin = node.path("pinOrigin").asText(null);
            return origin == null || origin.isBlank() ? "PUBLISHED" : origin;
        } catch (IOException ex) {
            return "PUBLISHED";
        }
    }

    private List<TemplateExportClauseSnapshotView> snapshotClauses(List<ContentModuleReferenceView> references) {
        List<TemplateExportClauseSnapshotView> snapshots = new ArrayList<>();
        if (references == null) {
            return snapshots;
        }
        for (ContentModuleReferenceView reference : references) {
            if (reference == null || reference.moduleId() == null || reference.moduleId().isBlank()) {
                continue;
            }
            UUID moduleId;
            try {
                moduleId = UUID.fromString(reference.moduleId());
            } catch (IllegalArgumentException ex) {
                continue;
            }
            Optional<ContentModuleEntity> moduleOpt =
                    contentModuleRepository.findByIdAndDeletedAtIsNull(moduleId);
            if (moduleOpt.isEmpty()) {
                continue;
            }
            ContentModuleEntity module = moduleOpt.get();
            Optional<ContentModuleVersionEntity> versionOpt = contentModuleVersionRepository
                    .findByModuleIdAndSemanticVersion(moduleId, reference.semanticVersion());
            if (versionOpt.isEmpty()) {
                continue;
            }
            ContentModuleVersionEntity moduleVersion = versionOpt.get();
            String semanticVersion = moduleVersion.getSemanticVersion();
            snapshots.add(new TemplateExportClauseSnapshotView(
                    module.getModuleCode(),
                    moduleVersion.getId().toString(),
                    parseVersionNumber(semanticVersion),
                    moduleVersion.getContentStructureJson(),
                    reference.locked(),
                    moduleVersion.getJurisdiction(),
                    formatDate(moduleVersion.getEffectiveFrom()),
                    formatDate(moduleVersion.getEffectiveTo()),
                    moduleVersion.getLegalReviewRef(),
                    semanticVersion,
                    module.getId().toString()
            ));
        }
        return snapshots;
    }

    private static int parseVersionNumber(String semanticVersion) {
        if (semanticVersion == null || semanticVersion.isBlank()) {
            return 1;
        }
        String major = semanticVersion.split("\\.")[0].replaceAll("[^0-9]", "");
        if (major.isBlank()) {
            return 1;
        }
        try {
            int value = Integer.parseInt(major);
            return Math.max(1, value);
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    private static String formatDate(java.time.Instant instant) {
        if (instant == null) {
            return null;
        }
        return ISO_DATE.format(instant);
    }

    private TemplateExportRenderProfileView resolveRenderProfile(TemplateVersionEntity version) {
        if (version.getRenderProfileJson() == null || version.getRenderProfileJson().isBlank()) {
            return null;
        }
        String profileVersion = version.getRenderProfileVersion();
        if (profileVersion == null || profileVersion.isBlank()) {
            profileVersion = "rp-v1";
        }
        return new TemplateExportRenderProfileView(profileVersion, version.getRenderProfileJson());
    }

    private List<TemplateExportAssetKeyManifestItemView> collectAssetKeys(
            List<AnchorBindingView> bindings,
            List<TemplateExportClauseSnapshotView> clauses
    ) {
        Map<String, TemplateExportAssetKeyUsage> keys = new LinkedHashMap<>();
        if (bindings != null) {
            for (AnchorBindingView binding : bindings) {
                if (binding == null) {
                    continue;
                }
                if ("IMAGE".equalsIgnoreCase(binding.declaredContentType())
                        && binding.structuredContentJson() != null) {
                    extractKeys(binding.structuredContentJson(), keys);
                } else if (binding.structuredContentJson() != null) {
                    extractKeys(binding.structuredContentJson(), keys);
                }
            }
        }
        for (TemplateExportClauseSnapshotView clause : clauses) {
            if (clause != null && clause.contentStructureJson() != null) {
                extractKeys(clause.contentStructureJson(), keys);
            }
        }
        List<TemplateExportAssetKeyManifestItemView> manifest = new ArrayList<>();
        for (Map.Entry<String, TemplateExportAssetKeyUsage> entry : keys.entrySet()) {
            manifest.add(new TemplateExportAssetKeyManifestItemView(entry.getKey(), entry.getValue()));
        }
        return manifest;
    }

    private void extractKeys(String json, Map<String, TemplateExportAssetKeyUsage> keys) {
        try {
            JsonNode root = objectMapper.readTree(json);
            walkAssetNodes(root, keys);
        } catch (IOException ignored) {
            // Non-JSON structured content — skip asset extraction for this payload.
        }
    }

    private void walkAssetNodes(JsonNode node, Map<String, TemplateExportAssetKeyUsage> keys) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            String type = node.path("type").asText("");
            if ("imageRef".equals(type)) {
                String ref = firstNonBlank(node, "imageRef", "referenceKey");
                putKey(keys, ref, TemplateExportAssetKeyUsage.IMAGE);
            } else if ("sealRef".equals(type)) {
                String ref = firstNonBlank(node, "referenceKey", "sealRef");
                putKey(keys, ref, TemplateExportAssetKeyUsage.OTHER);
            } else if (node.hasNonNull("imageRef")) {
                putKey(keys, node.get("imageRef").asText(), TemplateExportAssetKeyUsage.IMAGE);
            }
            node.fields().forEachRemaining(entry -> walkAssetNodes(entry.getValue(), keys));
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                walkAssetNodes(child, keys);
            }
        }
    }

    private static String firstNonBlank(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                String text = value.asText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
        }
        return null;
    }

    private static void putKey(
            Map<String, TemplateExportAssetKeyUsage> keys,
            String referenceKey,
            TemplateExportAssetKeyUsage usage
    ) {
        if (referenceKey == null || referenceKey.isBlank()) {
            return;
        }
        keys.putIfAbsent(referenceKey.trim(), usage);
    }

    private record ResolvedMaster(TemplateExportMasterPinView pin, byte[] bytes) {
    }
}
