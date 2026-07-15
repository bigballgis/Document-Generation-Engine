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
import com.bank.docgen.template.api.TemplateExportClauseSnapshotView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.api.TemplateImportDependencyItemView;
import com.bank.docgen.template.api.TemplateImportDependencyReportView;
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
 * CE-E01: builds import dependency pre-check reports for dry-run and commit gates.
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
            boolean requireMasterDocxForCommit
    ) {
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
        evaluateAssets(bundle, items);
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
            items.add(item(
                    TemplateImportDependencyType.MASTER_PIN,
                    TemplateImportDependencySeverity.MISMATCH,
                    "MASTER_FINGERPRINT_MISMATCH",
                    "api.error.template.dep.masterFingerprintMismatch",
                    null
            ));
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
            // Prefer moduleCode via snapshot coverage (source UUID is not a target identity).
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

    /**
     * Resolve clause identity by moduleCode: snapshot sourceModuleId correlation first,
     * then target lookup by UUID only when the module already exists locally.
     */
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

    private void evaluateAssets(TemplateExportBundleView bundle, List<TemplateImportDependencyItemView> items) {
        if (!TemplateExportV2Support.EXPORT_FORMAT_V2.equals(bundle.format())) {
            return;
        }
        List<TemplateExportAssetKeyManifestItemView> manifest =
                bundle.assetKeyManifest() == null ? List.of() : bundle.assetKeyManifest();
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
