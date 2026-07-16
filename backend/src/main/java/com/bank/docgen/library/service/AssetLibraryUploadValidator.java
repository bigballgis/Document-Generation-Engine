package com.bank.docgen.library.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.web.multipart.MultipartFile;

final class AssetLibraryUploadValidator {

    static final long MAX_BYTES = 5L * 1024L * 1024L;
    private static final Pattern ASSET_KEY_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9._-]{0,127}$");
    private static final byte[] PNG_MAGIC = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private AssetLibraryUploadValidator() {
    }

    static String normalizeAssetKey(String assetKey) {
        if (assetKey == null || assetKey.isBlank()) {
            throw invalidKey();
        }
        String trimmed = assetKey.trim();
        if (!ASSET_KEY_PATTERN.matcher(trimmed).matches()
                || trimmed.contains("..")
                || trimmed.indexOf('/') >= 0
                || trimmed.indexOf('\\') >= 0) {
            throw invalidKey();
        }
        return trimmed;
    }

    static ValidatedPayload validateFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new AssetLibraryValidationException(
                    ApiErrorCodes.ASSET_LIBRARY_CONTENT_TYPE_MISMATCH,
                    "api.error.assetLibrary.payloadEmpty"
            );
        }
        if (file.getSize() > MAX_BYTES) {
            throw new AssetLibraryValidationException(
                    ApiErrorCodes.ASSET_LIBRARY_PAYLOAD_TOO_LARGE,
                    "api.error.assetLibrary.payloadTooLarge"
            );
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception ex) {
            throw new AssetLibraryValidationException(
                    ApiErrorCodes.ASSET_LIBRARY_CONTENT_TYPE_MISMATCH,
                    "api.error.assetLibrary.contentTypeMismatch"
            );
        }
        if (bytes.length == 0 || bytes.length > MAX_BYTES) {
            throw new AssetLibraryValidationException(
                    ApiErrorCodes.ASSET_LIBRARY_PAYLOAD_TOO_LARGE,
                    "api.error.assetLibrary.payloadTooLarge"
            );
        }

        String declared = normalizeDeclaredContentType(file.getContentType());
        String detected = detectContentType(bytes);
        if (detected == null) {
            if (declared != null && !isAllowedImageType(declared)) {
                throw unsupportedType();
            }
            throw mismatch();
        }
        if (declared != null && !declared.equals(detected)) {
            if (!isAllowedImageType(declared)) {
                throw unsupportedType();
            }
            throw mismatch();
        }
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "asset" + extensionFor(detected);
        }
        return new ValidatedPayload(bytes, detected, originalFileName);
    }

    private static String normalizeDeclaredContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        int semicolon = normalized.indexOf(';');
        if (semicolon >= 0) {
            normalized = normalized.substring(0, semicolon).trim();
        }
        if ("application/octet-stream".equals(normalized)) {
            return null;
        }
        if ("image/jpg".equals(normalized)) {
            return "image/jpeg";
        }
        return normalized;
    }

    private static boolean isAllowedImageType(String contentType) {
        return "image/png".equals(contentType) || "image/jpeg".equals(contentType);
    }

    private static String detectContentType(byte[] bytes) {
        if (startsWith(bytes, PNG_MAGIC)) {
            return "image/png";
        }
        if (startsWith(bytes, JPEG_MAGIC)) {
            return "image/jpeg";
        }
        return null;
    }

    private static boolean startsWith(byte[] bytes, byte[] magic) {
        if (bytes.length < magic.length) {
            return false;
        }
        return Arrays.equals(bytes, 0, magic.length, magic, 0, magic.length);
    }

    private static String extensionFor(String contentType) {
        return "image/png".equals(contentType) ? ".png" : ".jpg";
    }

    private static AssetLibraryValidationException invalidKey() {
        return new AssetLibraryValidationException(
                ApiErrorCodes.ASSET_LIBRARY_ASSET_KEY_INVALID,
                "api.error.assetLibrary.assetKeyInvalid"
        );
    }

    private static AssetLibraryValidationException unsupportedType() {
        return new AssetLibraryValidationException(
                ApiErrorCodes.ASSET_LIBRARY_CONTENT_TYPE_UNSUPPORTED,
                "api.error.assetLibrary.contentTypeUnsupported"
        );
    }

    private static AssetLibraryValidationException mismatch() {
        return new AssetLibraryValidationException(
                ApiErrorCodes.ASSET_LIBRARY_CONTENT_TYPE_MISMATCH,
                "api.error.assetLibrary.contentTypeMismatch"
        );
    }

    record ValidatedPayload(byte[] bytes, String contentType, String originalFileName) {
    }
}
