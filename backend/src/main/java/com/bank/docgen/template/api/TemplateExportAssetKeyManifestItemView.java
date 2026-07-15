package com.bank.docgen.template.api;

import com.bank.docgen.template.domain.TemplateExportAssetKeyUsage;

/**
 * CE-E01 asset key inventory item (key only; no binary payload).
 */
public record TemplateExportAssetKeyManifestItemView(
        String referenceKey,
        TemplateExportAssetKeyUsage usage
) {
}
