package com.bank.docgen.library.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.bank.docgen.template.api.TemplateExportAssetKeyManifestItemView;
import java.time.Instant;
import java.util.List;

/**
 * Root {@code library-export-manifest.json} for {@code template-library-export-v1-zip}.
 */
public record LibraryExportManifestView(
        String format,
        String exportBatchId,
        Instant exportedAt,
        int bundleVersion,
        LibraryExportActorView actor,
        LibraryExportScopeView scope,
        LibraryExportCountsView counts,
        List<LibraryExportTemplateEntryView> templates,
        List<LibraryExportMasterCatalogEntryView> masterCatalog,
        List<LibraryExportClauseCatalogEntryView> clauseCatalog,
        List<TemplateExportAssetKeyManifestItemView> assetKeyManifest
) {
    public LibraryExportManifestView {
        templates = DefensiveCopies.copyList(templates);
        masterCatalog = DefensiveCopies.copyList(masterCatalog);
        clauseCatalog = DefensiveCopies.copyList(clauseCatalog);
        assetKeyManifest = DefensiveCopies.copyList(assetKeyManifest);
    }
}
