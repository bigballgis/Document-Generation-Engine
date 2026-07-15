package com.bank.docgen.rendering;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Locale;

/**
 * Parsed {@code qrBarcodeRef} node configuration (CE-K06b / K06b-C4).
 */
final class QrBarcodeRefConfig {

    static final int DEFAULT_SIZE_PX = 128;
    static final int MIN_SIZE_PX = 32;
    static final int MAX_SIZE_PX = 512;

    enum ErrorCorrection {
        L,
        M,
        Q,
        H
    }

    enum BarcodeFormatKind {
        QR_CODE,
        CODE_128
    }

    private final int sizePx;
    private final ErrorCorrection errorCorrection;
    private final BarcodeFormatKind format;

    QrBarcodeRefConfig(int sizePx, ErrorCorrection errorCorrection, BarcodeFormatKind format) {
        this.sizePx = sizePx;
        this.errorCorrection = errorCorrection;
        this.format = format;
    }

    int sizePx() {
        return sizePx;
    }

    ErrorCorrection errorCorrection() {
        return errorCorrection;
    }

    BarcodeFormatKind format() {
        return format;
    }

    static QrBarcodeRefConfig parse(JsonNode node) {
        int sizePx = DEFAULT_SIZE_PX;
        if (node.has("sizePx") && !node.get("sizePx").isNull()) {
            JsonNode sizeNode = node.get("sizePx");
            if (!sizeNode.isIntegralNumber() && !sizeNode.isFloatingPointNumber()) {
                throw configInvalid("sizePx must be an integer");
            }
            sizePx = sizeNode.asInt();
            if (sizePx < MIN_SIZE_PX || sizePx > MAX_SIZE_PX) {
                throw configInvalid("sizePx must be between " + MIN_SIZE_PX + " and " + MAX_SIZE_PX);
            }
        }

        ErrorCorrection errorCorrection = ErrorCorrection.M;
        if (node.has("errorCorrection") && !node.get("errorCorrection").isNull()) {
            String rawEc = node.get("errorCorrection").asText("").trim();
            if (!rawEc.isEmpty()) {
                try {
                    errorCorrection = ErrorCorrection.valueOf(rawEc.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ex) {
                    throw configInvalid("unsupported errorCorrection: " + rawEc);
                }
            }
        }

        BarcodeFormatKind format = BarcodeFormatKind.QR_CODE;
        if (node.has("format") && !node.get("format").isNull()) {
            String rawFormat = node.get("format").asText("").trim();
            if (!rawFormat.isEmpty()) {
                String normalized = rawFormat.toUpperCase(Locale.ROOT);
                if ("QR_CODE".equals(normalized)) {
                    format = BarcodeFormatKind.QR_CODE;
                } else if ("CODE_128".equals(normalized)) {
                    format = BarcodeFormatKind.CODE_128;
                } else {
                    throw configInvalid("unsupported format: " + rawFormat);
                }
            }
        }

        return new QrBarcodeRefConfig(sizePx, errorCorrection, format);
    }

    private static DocxAssemblyException configInvalid(String detail) {
        return new DocxAssemblyException(
                "api.error.rendering.qrBarcodeConfigInvalid",
                "Invalid qrBarcodeRef configuration: " + detail
        );
    }
}
