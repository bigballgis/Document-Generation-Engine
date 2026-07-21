package com.bank.docgen.template.service;

import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.master.persistence.MasterRevisionLineEntity;
import com.bank.docgen.master.persistence.MasterRevisionLineRepository;
import com.bank.docgen.master.service.MasterNotFoundException;
import com.bank.docgen.template.api.ContentModuleReferenceView;
import com.bank.docgen.template.api.TemplateExportAssetKeyManifestItemView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportClauseNestingGraphEdgeView;
import com.bank.docgen.template.api.TemplateExportClauseNestingGraphView;
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.api.TemplateImportDependencyItemView;
import com.bank.docgen.template.api.TemplateImportDependencyReportView;
import com.bank.docgen.template.domain.TemplateDependencyClosure;
import com.bank.docgen.template.domain.TemplateImportDependencySeverity;
import com.bank.docgen.template.domain.TemplateImportDependencyType;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * CE-E01 / Wave 7: builds import dependency pre-check reports for dry-run and commit gates.
 */
@Component
public class TemplateImportDependencyPrecheck {

    private final MasterDocumentRepository masterDocumentRepository;
    private final MasterRevisionLineRepository masterRevisionLineRepository;
    private final ObjectStoragePort objectStoragePort;
    private final ContentModuleRepository contentModuleRepository;
    private final ContentModuleVersionRepository contentModuleVersionRepository;

    public TemplateImportDependencyPrecheck(
            MasterDocumentRepository masterDocumentRepository,
            MasterRevisionLineRepository masterRevisionLineRepository,
            ObjectStoragePort objectStoragePort,
            ContentModuleRepository contentModuleRepository,
            ContentModuleVersionRepository contentModuleVersionRepository
    ) {
        this.masterDocumentRepository = masterDocumentRepository;
        this.masterRevisionLineRepository = masterRevisionLineRepository;
        this.objectStoragePort = objectStoragePort;
        this.contentModuleRepository = contentModuleRepository;
        this.contentModuleVersionRepository = contentModuleVersionRepository;
    }

    public record PrecheckContext(
            TemplateExportBundleView bundle,
            UUID targetMasterId,
            byte[] embeddedMasterDocx,
            boolean zipCarrier,
            boolean requireMasterDocxForCommit,
            Map<String, byte[]> embeddedAssetBinaries
    ) {
        public PrecheckContext {
            embeddedAssetBinaries = embeddedAssetBinaries == null ? Map.of() : Map.copyOf(embeddedAssetBinaries);
        }

        /** Backward-compatible CE-E01 constructor (no embedded assets). */
        public PrecheckContext(
                TemplateExportBundleView bundle,
                UUID targetMasterId,
                byte[] embeddedMasterDocx,
                boolean zipCarrier,
                boolean requireMasterDocxForCommit
        ) {
            this(bundle, targetMasterId, embeddedMasterDocx, zipCarrier, requireMasterDocxForCommit, Map.of());
        }

        /**
         * Prefer explicit bundle {@code dependencyClosure=PROMOTION} (Wave 7 export marker).
         * Fall back to embedded-asset / nesting-graph heuristics for legacy packs without the field.
         * Unknown explicit values fail closed (not treated as promotion).
         */
        public boolean promotionCarrier() {
            if (bundle != null
                    && bundle.dependencyClosure() != null
                    && !bundle.dependencyClosure().isBlank()) {
                try {
                    return TemplateDependencyClosure.parseOptional(bundle.dependencyClosure())
                            == TemplateDependencyClosure.PROMOTION;
                } catch (IllegalArgumentException ex) {
                    return false;
                }
            }
            if (embeddedAssetBinaries != null && !embeddedAssetBinaries.isEmpty()) {
                return true;
            }
            TemplateExportClauseNestingGraphView graph = bundle == null ? null : bundle.clauseNestingGraph();
            return graph != null && graph.edges() != null && !graph.edges().isEmpty();
        }
    }

    public TemplateImportDependencyReportView evaluate(PrecheckContext context) {
        List<TemplateImportDependencyItemView> items = new ArrayList<>();
        TemplateExportBundleView bundle = context.bundle();
        String format = bundle.format();

        items.add(item(
                TemplateImportDependencyType.BUNDLE_FORMAT,
                TemplateImportDependencySeverity.OK,
                "BUNDLE_FORMAT_OK",
                "api.error.template.dep.bundleFormatOk",
                format
        ));

        evaluateMasterPin(context, items);
        evaluateClauses(bundle, items);
        evaluateNesting(bundle, items);
        evaluateAssets(context, items);
        evaluateRenderProfile(bundle, items);

        int blocking = 0;
        int warning = 0;
        int info = 0;
        for (TemplateImportDependencyItemView entry : items) {
            if (entry.severity() == TemplateImportDependencySeverity.MISSING
                    || entry.severity() == TemplateImportDependencySeverity.MISMATCH) {
                blocking++;
            } else if (entry.severity() == TemplateImportDependencySeverity.INFO) {
                info++;
            } else if (entry.severity() == TemplateImportDependencySeverity.WILL_MATERIALIZE) {
                warning++;
            }
        }
        return new TemplateImportDependencyReportView(
                items,
                blocking,
                warning,
                info,
                blocking == 0,
                format
        );
    }

    public void assertMasterGate(UUID targetMasterId, String expectedGroupCode) {
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(targetMasterId)
                .orElseThrow(MasterNotFoundException::new);
        if (master.getStatus() != MasterDocumentStatus.APPROVED) {
            throw new TemplateValidationException("api.error.template.masterNotApproved");
        }
        if (!master.getGroupCode().equals(expectedGroupCode)) {
            throw new TemplateValidationException("api.error.template.masterGroupMismatch");
        }
    }

    public String resolveTargetMasterFileHash(UUID targetMasterId) {
        MasterDocumentEntity master = masterDocumentRepository.findByIdAndDeletedAtIsNull(targetMasterId)
                .orElseThrow(MasterNotFoundException::new);
        UUID revisionId = master.getCurrentRevisionLineId();
        if (revisionId == null) {
            throw new TemplateValidationException("api.error.template.masterNotApproved");
        }
        MasterRevisionLineEntity revision = masterRevisionLineRepository
                .findByIdAndMasterIdAndDeletedAtIsNull(revisionId, targetMasterId)
                .orElseThrow(MasterNotFoundException::new);
        try (InputStream stream = objectStoragePort.get(revision.getStorageKey())) {
            return TemplateExportHashSupport.sha256Hex(stream.readAllBytes());
        } catch (IOException | RuntimeException ex) {
            throw new TemplateValidationException("api.error.template.masterNotApproved");
        }
    }

    private void evaluateMasterPin(PrecheckContext context, List<TemplateImportDependencyItemView> items) {
        TemplateExportBundleView bundle = context.bundle();
        if (!TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())) {
            return;
        }
        TemplateExportMasterPinView pin = bundle.masterPin();
        if (pin == null || pin.masterFileHash() == null || pin.masterFileHash().isBlank()) {
            items.add(item(
                    TemplateImportDependencyType.MASTER_PIN,
                    TemplateImportDependencySeverity.MISSING,
                    "MASTER_PIN_ABSENT",
                    "api.error.template.dep.masterPinAbsent",
                    null
            ));
            return;
        }
        boolean hasDocx = context.embeddedMasterDocx() != null && context.embeddedMasterDocx().length > 0;
        if (!hasDocx) {
            items.add(item(
                    TemplateImportDependencyType.MASTER_PIN,
                    TemplateImportDependencySeverity.MISSING,
                    "MASTER_DOCX_ABSENT",
                    "api.error.template.dep.masterDocxAbsent",
                    context.zipCarrier() ? null : "JSON-only v2 carrier"
            ));
        } else {
            String embeddedHash = TemplateExportHashSupport.sha256Hex(context.embeddedMasterDocx());
            if (!embeddedHash.equalsIgnoreCase(pin.masterFileHash())) {
                items.add(item(
                        TemplateImportDependencyType.MASTER_PIN,
                        TemplateImportDependencySeverity.MISMATCH,
                        "MASTER_DOCX_HASH_MISMATCH",
                        "api.error.template.dep.masterDocxHashMismatch",
                        null
                ));
            }
        }

        String targetHash = resolveTargetMasterFileHash(context.targetMasterId());
        if (!targetHash.equalsIgnoreCase(pin.masterFileHash())) {
            if (context.promotionCarrier() && hasDocx) {
                items.add(item(
                        TemplateImportDependencyType.MASTER_PIN,
                        TemplateImportDependencySeverity.WILL_MATERIALIZE,
                        "MASTER_WILL_MATERIALIZE",
                        "api.error.template.dep.masterWillMaterialize",
                        null
                ));
            } else {
                items.add(item(
                        TemplateImportDependencyType.MASTER_PIN,
                        TemplateImportDependencySeverity.MISMATCH,
                        "MASTER_FINGERPRINT_MISMATCH",
                        "api.error.template.dep.masterFingerprintMismatch",
                        null
                ));
            }
        } else {
            items.add(item(
                    TemplateImportDependencyType.MASTER_PIN,
                    TemplateImportDependencySeverity.OK,
                    "MASTER_FINGERPRINT_OK",
                    "api.error.template.dep.masterFingerprintOk",
                    null
            ));
        }
    }

    private void evaluateClauses(TemplateExportBundleView bundle, List<TemplateImportDependencyItemView> items) {
        if (!TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())) {
            return;
        }
        List<TemplateExportClauseSnapshotView> snapshots =
                bundle.clauseSnapshots() == null ? List.of() : bundle.clauseSnapshots();
        Map<String, TemplateExportClauseSnapshotView> snapshotsByCode = new LinkedHashMap<>();
        Map<String, TemplateExportClauseSnapshotView> snapshotsBySourceModuleId = new LinkedHashMap<>();
        for (TemplateExportClauseSnapshotView snapshot : snapshots) {
            if (snapshot == null || snapshot.moduleCode() == null || snapshot.moduleCode().isBlank()) {
                continue;
            }
            String code = snapshot.moduleCode().trim().toUpperCase(Locale.ROOT);
            snapshotsByCode.put(code, snapshot);
            if (snapshot.sourceModuleId() != null && !snapshot.sourceModuleId().isBlank()) {
                snapshotsBySourceModuleId.put(snapshot.sourceModuleId().trim(), snapshot);
            }
        }

        for (Map.Entry<String, TemplateExportClauseSnapshotView> entry : snapshotsByCode.entrySet()) {
            String moduleCode = entry.getKey();
            TemplateExportClauseSnapshotView snapshot = entry.getValue();
            Optional<ContentModuleEntity> existing =
                    contentModuleRepository.findByModuleCodeAndDeletedAtIsNull(moduleCode);
            if (existing.isPresent()
                    && hasCompatibleVersion(existing.get().getId(), snapshot)) {
                items.add(item(
                        TemplateImportDependencyType.CLAUSE,
                        TemplateImportDependencySeverity.OK,
                        "CLAUSE_PRESENT",
                        "api.error.template.dep.clausePresent",
                        moduleCode
                ));
            } else {
                items.add(item(
                        TemplateImportDependencyType.CLAUSE,
                        TemplateImportDependencySeverity.WILL_MATERIALIZE,
                        "CLAUSE_WILL_MATERIALIZE",
                        "api.error.template.dep.clauseWillMaterialize",
                        moduleCode
                ));
            }
        }

        List<ContentModuleReferenceView> references =
                bundle.contentModuleReferences() == null ? List.of() : bundle.contentModuleReferences();
        for (ContentModuleReferenceView reference : references) {
            if (reference == null) {
                continue;
            }
            String moduleCode = resolveModuleCode(reference, snapshotsBySourceModuleId);
            if (moduleCode != null && snapshotsByCode.containsKey(moduleCode)) {
                continue;
            }
            if (moduleCode != null) {
                Optional<ContentModuleEntity> existing =
                        contentModuleRepository.findByModuleCodeAndDeletedAtIsNull(moduleCode);
                if (existing.isPresent()) {
                    items.add(item(
                            TemplateImportDependencyType.CLAUSE,
                            TemplateImportDependencySeverity.OK,
                            "CLAUSE_PRESENT",
                            "api.error.template.dep.clausePresent",
                            moduleCode
                    ));
                    continue;
                }
            }
            items.add(item(
                    TemplateImportDependencyType.CLAUSE,
                    TemplateImportDependencySeverity.MISSING,
                    "CLAUSE_MISSING",
                    "api.error.template.dep.clauseMissing",
                    moduleCode == null ? reference.moduleId() : moduleCode
            ));
        }
    }

    private void evaluateNesting(TemplateExportBundleView bundle, List<TemplateImportDependencyItemView> items) {
        if (!TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())) {
            return;
        }
        TemplateExportClauseNestingGraphView graph = bundle.clauseNestingGraph();
        if (graph == null || graph.edges() == null || graph.edges().isEmpty()) {
            return;
        }
        Map<String, TemplateExportClauseSnapshotView> snapshotsByCode = new LinkedHashMap<>();
        List<TemplateExportClauseSnapshotView> snapshots =
                bundle.clauseSnapshots() == null ? List.of() : bundle.clauseSnapshots();
        for (TemplateExportClauseSnapshotView snapshot : snapshots) {
            if (snapshot == null || snapshot.moduleCode() == null || snapshot.moduleCode().isBlank()) {
                continue;
            }
            snapshotsByCode.put(snapshot.moduleCode().trim().toUpperCase(Locale.ROOT), snapshot);
        }
        for (TemplateExportClauseNestingGraphEdgeView edge : graph.edges()) {
            if (edge == null) {
                continue;
            }
            String parent = edge.parentModuleCode() == null
                    ? ""
                    : edge.parentModuleCode().trim().toUpperCase(Locale.ROOT);
            String child = edge.childModuleCode() == null
                    ? ""
                    : edge.childModuleCode().trim().toUpperCase(Locale.ROOT);
            boolean parentOnTarget = !parent.isBlank()
                    && contentModuleRepository.findByModuleCodeAndDeletedAtIsNull(parent).isPresent();
            boolean childOnTarget = !child.isBlank()
                    && contentModuleRepository.findByModuleCodeAndDeletedAtIsNull(child).isPresent();
            boolean parentSnapshot = snapshotsByCode.containsKey(parent);
            boolean childSnapshot = snapshotsByCode.containsKey(child);
            if (parentOnTarget && childOnTarget) {
                items.add(item(
                        TemplateImportDependencyType.CLAUSE_NESTING,
                        TemplateImportDependencySeverity.OK,
                        "CLAUSE_NESTING_OK",
                        "api.error.template.dep.clauseNestingOk",
                        parent + ">" + child
                ));
            } else if ((parentOnTarget || parentSnapshot) && (childOnTarget || childSnapshot)) {
                items.add(item(
                        TemplateImportDependencyType.CLAUSE_NESTING,
                        TemplateImportDependencySeverity.WILL_MATERIALIZE,
                        "CLAUSE_NESTING_WILL_MATERIALIZE",
                        "api.error.template.dep.clauseNestingWillMaterialize",
                        parent + ">" + child
                ));
            } else {
                items.add(item(
                        TemplateImportDependencyType.CLAUSE_NESTING,
                        TemplateImportDependencySeverity.MISSING,
                        "CLAUSE_NESTING_MISSING",
                        "api.error.template.dep.clauseNestingMissing",
                        parent + ">" + child
                ));
            }
        }
    }

    private String resolveModuleCode(
            ContentModuleReferenceView reference,
            Map<String, TemplateExportClauseSnapshotView> snapshotsBySourceModuleId
    ) {
        if (reference.moduleId() != null && !reference.moduleId().isBlank()) {
            TemplateExportClauseSnapshotView fromSnapshot =
                    snapshotsBySourceModuleId.get(reference.moduleId().trim());
            if (fromSnapshot != null && fromSnapshot.moduleCode() != null) {
                return fromSnapshot.moduleCode().trim().toUpperCase(Locale.ROOT);
            }
        }
        if (reference.moduleId() == null || reference.moduleId().isBlank()) {
            return null;
        }
        try {
            return contentModuleRepository.findByIdAndDeletedAtIsNull(UUID.fromString(reference.moduleId()))
                    .map(ContentModuleEntity::getModuleCode)
                    .map(code -> code.toUpperCase(Locale.ROOT))
                    .orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean hasCompatibleVersion(UUID moduleId, TemplateExportClauseSnapshotView snapshot) {
        String full = snapshot.semanticVersion();
        if (full != null && !full.isBlank()) {
            return contentModuleVersionRepository
                    .findByModuleIdAndSemanticVersion(moduleId, full.trim())
                    .isPresent();
        }
        return contentModuleVersionRepository.findByModuleIdOrderBySemanticVersionDesc(moduleId).stream()
                .anyMatch(version -> compatibleVersion(version.getSemanticVersion(), snapshot.versionNumber()));
    }

    private static boolean compatibleVersion(String semanticVersion, int versionNumber) {
        if (semanticVersion == null || semanticVersion.isBlank()) {
            return false;
        }
        String major = semanticVersion.split("\\.")[0].replaceAll("[^0-9]", "");
        if (major.isBlank()) {
            return false;
        }
        try {
            return Integer.parseInt(major) == versionNumber;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private void evaluateAssets(PrecheckContext context, List<TemplateImportDependencyItemView> items) {
        TemplateExportBundleView bundle = context.bundle();
        if (!TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())) {
            return;
        }
        List<TemplateExportAssetKeyManifestItemView> manifest =
                bundle.assetKeyManifest() == null ? List.of() : bundle.assetKeyManifest();
        Map<String, byte[]> embedded = context.embeddedAssetBinaries();
        for (TemplateExportAssetKeyManifestItemView asset : manifest) {
            if (asset == null || asset.referenceKey() == null || asset.referenceKey().isBlank()) {
                continue;
            }
            String key = asset.referenceKey().trim();
            if (objectStorageExists(key)) {
                items.add(item(
                        TemplateImportDependencyType.ASSET_KEY,
                        TemplateImportDependencySeverity.OK,
                        "ASSET_KEY_PRESENT",
                        "api.error.template.dep.assetKeyPresent",
                        key
                ));
            } else if (embedded.containsKey(key) && embedded.get(key) != null && embedded.get(key).length > 0) {
                items.add(item(
                        TemplateImportDependencyType.ASSET_BINARY,
                        TemplateImportDependencySeverity.WILL_MATERIALIZE,
                        "ASSET_WILL_MATERIALIZE",
                        "api.error.template.dep.assetWillMaterialize",
                        key
                ));
            } else if (!embedded.isEmpty() || context.promotionCarrier()) {
                items.add(item(
                        TemplateImportDependencyType.ASSET_BINARY,
                        TemplateImportDependencySeverity.MISSING,
                        "ASSET_BINARY_ABSENT",
                        "api.error.template.dep.assetBinaryAbsent",
                        key
                ));
            } else {
                items.add(item(
                        TemplateImportDependencyType.ASSET_KEY,
                        TemplateImportDependencySeverity.MISSING,
                        "ASSET_KEY_MISSING",
                        "api.error.template.dep.assetKeyMissing",
                        key
                ));
            }
        }
    }

    private boolean objectStorageExists(String reference) {
        if (objectStoragePort.exists(reference)) {
            return true;
        }
        if (!reference.contains(".")) {
            return objectStoragePort.exists(reference + ".png")
                    || objectStoragePort.exists(reference + ".jpg")
                    || objectStoragePort.exists(reference + ".jpeg");
        }
        return false;
    }

    private void evaluateRenderProfile(
            TemplateExportBundleView bundle,
            List<TemplateImportDependencyItemView> items
    ) {
        if (!TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())) {
            return;
        }
        if (bundle.renderProfile() == null
                || bundle.renderProfile().json() == null
                || bundle.renderProfile().json().isBlank()) {
            items.add(item(
                    TemplateImportDependencyType.RENDER_PROFILE,
                    TemplateImportDependencySeverity.INFO,
                    "RENDER_PROFILE_ABSENT",
                    "api.error.template.dep.renderProfileAbsent",
                    null
            ));
        } else {
            items.add(item(
                    TemplateImportDependencyType.RENDER_PROFILE,
                    TemplateImportDependencySeverity.OK,
                    "RENDER_PROFILE_PRESENT",
                    "api.error.template.dep.renderProfilePresent",
                    bundle.renderProfile().version()
            ));
        }
    }

    private static TemplateImportDependencyItemView item(
            TemplateImportDependencyType type,
            TemplateImportDependencySeverity severity,
            String code,
            String messageKey,
            String detail
    ) {
        return new TemplateImportDependencyItemView(type, severity, code, messageKey, detail);
    }
}
