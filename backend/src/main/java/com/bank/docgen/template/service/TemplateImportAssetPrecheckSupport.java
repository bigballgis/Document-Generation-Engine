package com.bank.docgen.template.service;

import com.bank.docgen.infrastructure.storage.ObjectStoragePort;
import com.bank.docgen.template.api.TemplateExportAssetKeyManifestItemView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateImportDependencyItemView;
import com.bank.docgen.template.domain.TemplateImportDependencySeverity;
import com.bank.docgen.template.domain.TemplateImportDependencyType;
import java.util.List;
import java.util.Map;

/**
 * Package-private ASSET_KEY / ASSET_BINARY / RENDER_PROFILE evaluation for
 * {@link TemplateImportDependencyPrecheck}.
 */
final class TemplateImportAssetPrecheckSupport {

    private final ObjectStoragePort objectStoragePort;

    TemplateImportAssetPrecheckSupport(ObjectStoragePort objectStoragePort) {
        this.objectStoragePort = objectStoragePort;
    }

    void evaluateAssets(
            TemplateImportDependencyPrecheck.PrecheckContext context,
            List<TemplateImportDependencyItemView> items
    ) {
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

    void evaluateRenderProfile(
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
