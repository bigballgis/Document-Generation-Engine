package com.bank.docgen.library.api;

import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.domain.AssetLibraryAssetStatus;
import java.time.Instant;

public record AssetLibraryAssetView(
        String groupCode,
        String assetKey,
        AssetLibraryAssetClass assetClass,
        AssetLibraryAssetStatus status,
        String contentType,
        long sizeBytes,
        String contentSha256,
        String originalFileName,
        String uploadedBy,
        Instant uploadedAt
) {
}
