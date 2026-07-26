package com.bank.docgen.template.service;

import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.service.AssetLibraryService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateExportAssetKeyManifestItemView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.domain.TemplateExportAssetKeyUsage;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Package-private asset binary materialization + render-profile apply for
 * {@link TemplateImportService}.
 */
final class TemplateImportAssetMaterializeSupport {

    private final AssetLibraryService assetLibraryService;
    private final TemplateVersionRepository templateVersionRepository;

    TemplateImportAssetMaterializeSupport(
            AssetLibraryService assetLibraryService,
            TemplateVersionRepository templateVersionRepository
    ) {
        this.assetLibraryService = assetLibraryService;
        this.templateVersionRepository = templateVersionRepository;
    }

    void materializeAssets(
            TemplateExportBundleView bundle,
            Map<String, byte[]> embeddedAssetBinaries,
            ManagementSessionClaims session
    ) {
        if (embeddedAssetBinaries == null || embeddedAssetBinaries.isEmpty()) {
            return;
        }
        Map<String, TemplateExportAssetKeyUsage> usageByKey = new LinkedHashMap<>();
        List<TemplateExportAssetKeyManifestItemView> manifest =
                bundle.assetKeyManifest() == null ? List.of() : bundle.assetKeyManifest();
        for (TemplateExportAssetKeyManifestItemView item : manifest) {
            if (item != null && item.referenceKey() != null && !item.referenceKey().isBlank()) {
                usageByKey.put(item.referenceKey().trim(), item.usage());
            }
        }
        for (Map.Entry<String, byte[]> entry : embeddedAssetBinaries.entrySet()) {
            if (entry.getValue() == null || entry.getValue().length == 0) {
                continue;
            }
            TemplateExportAssetKeyUsage usage = usageByKey.getOrDefault(
                    entry.getKey(),
                    TemplateExportAssetKeyUsage.IMAGE
            );
            AssetLibraryAssetClass assetClass = usage == TemplateExportAssetKeyUsage.IMAGE
                    ? AssetLibraryAssetClass.IMAGE
                    : AssetLibraryAssetClass.OTHER;
            assetLibraryService.materializeImportedAsset(
                    session,
                    bundle.metadata() == null ? null : bundle.metadata().groupCode(),
                    entry.getKey(),
                    assetClass,
                    entry.getValue(),
                    "application/octet-stream",
                    entry.getKey()
            );
        }
    }

    void applyRenderProfile(UUID templateId, TemplateExportBundleView bundle) {
        if (bundle.renderProfile() == null
                || bundle.renderProfile().json() == null
                || bundle.renderProfile().json().isBlank()) {
            return;
        }
        List<TemplateVersionEntity> versions =
                templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId);
        if (versions.isEmpty()) {
            return;
        }
        TemplateVersionEntity version = versions.get(0);
        version.setRenderProfileJson(bundle.renderProfile().json());
        version.setRenderProfileVersion(
                bundle.renderProfile().version() == null ? "rp-v1" : bundle.renderProfile().version()
        );
        templateVersionRepository.save(version);
    }
}
