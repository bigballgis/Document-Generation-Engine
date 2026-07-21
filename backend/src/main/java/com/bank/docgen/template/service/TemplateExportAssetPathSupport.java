package com.bank.docgen.template.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Path-safe ZIP entry names for promotion asset binaries (PP-C1).
 * Manifest retains the original asset key; ZIP path uses encoding.
 */
public final class TemplateExportAssetPathSupport {

    public static final String ZIP_ASSET_DIR = "artifacts/assets/";
    public static final String LIBRARY_ASSET_DIR = "assets/";

    private TemplateExportAssetPathSupport() {
    }

    public static String zipEntryName(String assetKey) {
        return ZIP_ASSET_DIR + pathSafeSegment(assetKey);
    }

    public static String libraryEntryName(String assetKey) {
        return LIBRARY_ASSET_DIR + pathSafeSegment(assetKey);
    }

    public static String pathSafeSegment(String assetKey) {
        if (assetKey == null || assetKey.isBlank()) {
            return "_";
        }
        String encoded = URLEncoder.encode(assetKey.trim(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return encoded.replace("..", "_").replace("/", "_").replace("\\", "_");
    }

    /**
     * Reverse {@link #pathSafeSegment(String)} for import auto-detect (URLDecoder).
     */
    public static String decodePathSegment(String encodedSegment) {
        if (encodedSegment == null || encodedSegment.isBlank()) {
            return encodedSegment;
        }
        return java.net.URLDecoder.decode(encodedSegment, StandardCharsets.UTF_8);
    }
}
