package com.bank.docgen.template.service;

import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.contentmodule.service.ContentModuleGovernanceException;
import com.bank.docgen.contentmodule.service.ContentModuleNestingService;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.template.api.TemplateExportAssetKeyManifestItemView;
import com.bank.docgen.template.api.TemplateExportClauseNestingGraphEdgeView;
import com.bank.docgen.template.api.TemplateExportClauseNestingGraphView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * SYS-NORM Wave 7 — promotion dependency closure (asset binaries + clause nesting graph).
 */
@Component
public class TemplateExportPromotionSupport {

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

    private final ContentModuleRepository contentModuleRepository;
    private final ContentModuleVersionRepository contentModuleVersionRepository;
    private final ContentModuleNestingService nestingService;
    private final ObjectStoragePort objectStoragePort;

    public TemplateExportPromotionSupport(
            ContentModuleRepository contentModuleRepository,
            ContentModuleVersionRepository contentModuleVersionRepository,
            ContentModuleNestingService nestingService,
            ObjectStoragePort objectStoragePort
    ) {
        this.contentModuleRepository = contentModuleRepository;
        this.contentModuleVersionRepository = contentModuleVersionRepository;
        this.nestingService = nestingService;
        this.objectStoragePort = objectStoragePort;
    }

    public record PromotionArtifacts(
            List<TemplateExportClauseSnapshotView> clauseSnapshots,
            TemplateExportClauseNestingGraphView clauseNestingGraph,
            Map<String, byte[]> assetBinaries
    ) {
        public PromotionArtifacts {
            clauseSnapshots = clauseSnapshots == null ? List.of() : List.copyOf(clauseSnapshots);
            assetBinaries = assetBinaries == null ? Map.of() : Map.copyOf(assetBinaries);
        }
    }

    public PromotionArtifacts assemble(
            List<TemplateExportClauseSnapshotView> directSnapshots,
            List<TemplateExportAssetKeyManifestItemView> assetKeyManifest
    ) {
        NestingClosure nesting = expandNestingClosure(directSnapshots == null ? List.of() : directSnapshots);
        Map<String, byte[]> assets = loadAssetBinaries(assetKeyManifest == null ? List.of() : assetKeyManifest);
        return new PromotionArtifacts(nesting.snapshots(), nesting.graph(), assets);
    }

    private NestingClosure expandNestingClosure(List<TemplateExportClauseSnapshotView> directSnapshots) {
        Map<String, TemplateExportClauseSnapshotView> byCode = new LinkedHashMap<>();
        List<TemplateExportClauseNestingGraphEdgeView> edges = new ArrayList<>();
        Set<String> edgeKeys = new LinkedHashSet<>();
        int[] maxDepth = {0};

        for (TemplateExportClauseSnapshotView snapshot : directSnapshots) {
            if (snapshot == null || snapshot.moduleCode() == null || snapshot.moduleCode().isBlank()) {
                continue;
            }
            String code = snapshot.moduleCode().trim().toUpperCase(Locale.ROOT);
            byCode.putIfAbsent(code, snapshot);
        }

        for (String rootCode : List.copyOf(byCode.keySet())) {
            Set<String> path = new LinkedHashSet<>();
            path.add(rootCode);
            walkNesting(
                    rootCode,
                    byCode.get(rootCode).contentStructureJson(),
                    0,
                    path,
                    byCode,
                    edges,
                    edgeKeys,
                    maxDepth
            );
        }

        TemplateExportClauseNestingGraphView graph = edges.isEmpty()
                ? TemplateExportClauseNestingGraphView.empty()
                : new TemplateExportClauseNestingGraphView(edges, maxDepth[0]);
        return new NestingClosure(List.copyOf(byCode.values()), graph);
    }

    private void walkNesting(
            String parentCode,
            String structureJson,
            int depthFromRoot,
            Set<String> path,
            Map<String, TemplateExportClauseSnapshotView> byCode,
            List<TemplateExportClauseNestingGraphEdgeView> edges,
            Set<String> edgeKeys,
            int[] maxDepth
    ) {
        Set<String> nestedKeys;
        try {
            nestedKeys = nestingService.extractNestedReferenceKeys(structureJson);
        } catch (ContentModuleGovernanceException ex) {
            throw nestingFail(ex.errorCode(), ex.messageKey());
        }
        for (String nestedKey : nestedKeys) {
            if (nestedKey == null || nestedKey.isBlank()) {
                continue;
            }
            String childCode = nestedKey.trim().toUpperCase(Locale.ROOT);
            int nextDepth = depthFromRoot + 1;
            if (nextDepth > ContentModuleNestingService.MAX_NESTING_DEPTH) {
                throw nestingFail(
                        ApiErrorCodes.CONTENT_MODULE_NESTING_DEPTH_EXCEEDED,
                        "api.error.contentModule.nestingDepthExceeded"
                );
            }
            if (path.contains(childCode)) {
                throw nestingFail(
                        ApiErrorCodes.CONTENT_MODULE_NESTING_CYCLE,
                        "api.error.contentModule.nestingCycle"
                );
            }
            String edgeKey = parentCode + ">" + childCode;
            if (edgeKeys.add(edgeKey)) {
                edges.add(new TemplateExportClauseNestingGraphEdgeView(parentCode, childCode, nextDepth));
            }
            maxDepth[0] = Math.max(maxDepth[0], nextDepth);

            TemplateExportClauseSnapshotView childSnapshot = byCode.get(childCode);
            if (childSnapshot == null) {
                childSnapshot = snapshotByModuleCode(childCode);
                byCode.put(childCode, childSnapshot);
            }
            path.add(childCode);
            walkNesting(
                    childCode,
                    childSnapshot.contentStructureJson(),
                    nextDepth,
                    path,
                    byCode,
                    edges,
                    edgeKeys,
                    maxDepth
            );
            path.remove(childCode);
        }
    }

    private TemplateExportClauseSnapshotView snapshotByModuleCode(String moduleCode) {
        Optional<ContentModuleEntity> moduleOpt =
                contentModuleRepository.findByModuleCodeAndDeletedAtIsNull(moduleCode);
        if (moduleOpt.isEmpty()) {
            throw nestingFail(
                    ApiErrorCodes.CONTENT_MODULE_NESTING_TARGET_UNRESOLVED,
                    "api.error.contentModule.nestingTargetUnresolved"
            );
        }
        ContentModuleEntity module = moduleOpt.get();
        List<ContentModuleVersionEntity> versions =
                contentModuleVersionRepository.findByModuleIdOrderBySemanticVersionDesc(module.getId());
        if (versions.isEmpty()) {
            throw nestingFail(
                    ApiErrorCodes.CONTENT_MODULE_NESTING_TARGET_UNRESOLVED,
                    "api.error.contentModule.nestingTargetUnresolved"
            );
        }
        ContentModuleVersionEntity moduleVersion = versions.getFirst();
        String semanticVersion = moduleVersion.getSemanticVersion();
        return new TemplateExportClauseSnapshotView(
                module.getModuleCode(),
                moduleVersion.getId().toString(),
                parseVersionNumber(semanticVersion),
                moduleVersion.getContentStructureJson(),
                false,
                moduleVersion.getJurisdiction(),
                formatDate(moduleVersion.getEffectiveFrom()),
                formatDate(moduleVersion.getEffectiveTo()),
                moduleVersion.getLegalReviewRef(),
                semanticVersion,
                module.getId().toString()
        );
    }

    private Map<String, byte[]> loadAssetBinaries(List<TemplateExportAssetKeyManifestItemView> manifest) {
        Map<String, byte[]> binaries = new LinkedHashMap<>();
        for (TemplateExportAssetKeyManifestItemView item : manifest) {
            if (item == null || item.referenceKey() == null || item.referenceKey().isBlank()) {
                continue;
            }
            String key = item.referenceKey().trim();
            byte[] bytes = readAssetBytes(key);
            if (bytes.length == 0) {
                throw new TemplateGovernanceException(
                        ApiErrorCodes.EXPORT_ASSET_BINARY_MISSING,
                        "api.error.template.exportAssetBinaryMissing",
                        HttpStatus.UNPROCESSABLE_ENTITY
                );
            }
            binaries.put(key, bytes);
        }
        return binaries;
    }

    private byte[] readAssetBytes(String referenceKey) {
        String resolved = resolveExistingObjectKey(referenceKey);
        if (resolved.isEmpty()) {
            return new byte[0];
        }
        try (InputStream stream = objectStoragePort.get(resolved)) {
            byte[] bytes = stream.readAllBytes();
            return bytes == null ? new byte[0] : bytes;
        } catch (IOException | RuntimeException ex) {
            return new byte[0];
        }
    }

    private String resolveExistingObjectKey(String referenceKey) {
        if (objectStoragePort.exists(referenceKey)) {
            return referenceKey;
        }
        if (!referenceKey.contains(".")) {
            if (objectStoragePort.exists(referenceKey + ".png")) {
                return referenceKey + ".png";
            }
            if (objectStoragePort.exists(referenceKey + ".jpg")) {
                return referenceKey + ".jpg";
            }
            if (objectStoragePort.exists(referenceKey + ".jpeg")) {
                return referenceKey + ".jpeg";
            }
        }
        return "";
    }

    private static TemplateGovernanceException nestingFail(String code, String messageKey) {
        return new TemplateGovernanceException(code, messageKey, HttpStatus.UNPROCESSABLE_ENTITY);
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
            return Math.max(1, Integer.parseInt(major));
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

    private record NestingClosure(
            List<TemplateExportClauseSnapshotView> snapshots,
            TemplateExportClauseNestingGraphView graph
    ) {
    }
}
