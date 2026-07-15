package com.bank.docgen.rendering;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

/**
 * Emits {@code qrBarcodeRef} as an embedded PNG picture (CE-K06b).
 */
final class QrBarcodeRefDocxSupport {

    private QrBarcodeRefDocxSupport() {
    }

    static void writeQrBarcodeRef(
            JsonNode node,
            XWPFParagraph paragraph,
            Map<String, Object> variables,
            StructuredContentDocxStyleSupport styles
    ) {
        QrBarcodeRefConfig config = QrBarcodeRefConfig.parse(node);
        String payload = resolvePayload(node, variables);
        byte[] png = QrBarcodePngEncoder.encode(payload, config);
        String referenceKey = node.path("referenceKey").asText("qr").trim();
        String fileName = sanitizeFileName(referenceKey) + ".png";
        int emu = Units.pixelToEMU(config.sizePx());
        try {
            XWPFRun run = paragraph.createRun();
            styles.applyDefaultRunStyle(run);
            run.addPicture(
                    new java.io.ByteArrayInputStream(png),
                    XWPFDocument.PICTURE_TYPE_PNG,
                    fileName,
                    emu,
                    emu
            );
        } catch (DocxAssemblyException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DocxAssemblyException(ex);
        }
    }

    static String resolvePayload(JsonNode node, Map<String, Object> variables) {
        String referenceKey = node.path("referenceKey").asText("").trim();
        if (referenceKey.isEmpty()) {
            throw payloadMissing();
        }
        if (variables == null || !variables.containsKey(referenceKey)) {
            throw payloadMissing();
        }
        Object value = variables.get(referenceKey);
        if (value == null) {
            throw payloadMissing();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw payloadMissing();
        }
        return text;
    }

    private static DocxAssemblyException payloadMissing() {
        return new DocxAssemblyException(
                "api.error.rendering.qrBarcodePayloadMissing",
                "qrBarcodeRef payload is missing or blank for the referenced variable key"
        );
    }

    private static String sanitizeFileName(String referenceKey) {
        String sanitized = referenceKey.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isBlank() ? "qr-barcode" : sanitized;
    }
}
