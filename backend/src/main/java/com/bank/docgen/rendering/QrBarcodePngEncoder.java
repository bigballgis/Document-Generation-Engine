package com.bank.docgen.rendering;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * ZXing-backed PNG encoder for {@code qrBarcodeRef} (CE-K06b).
 */
final class QrBarcodePngEncoder {

    private QrBarcodePngEncoder() {
    }

    static byte[] encode(String payload, QrBarcodeRefConfig config) {
        try {
            BitMatrix matrix = encodeMatrix(payload, config);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "PNG", output)) {
                throw encodeFailed("PNG writer unavailable");
            }
            return output.toByteArray();
        } catch (DocxAssemblyException ex) {
            throw ex;
        } catch (WriterException | IOException | IllegalArgumentException ex) {
            throw encodeFailed(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), ex);
        }
    }

    private static BitMatrix encodeMatrix(String payload, QrBarcodeRefConfig config) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);
        int sizePx = config.sizePx();
        if (config.format() == QrBarcodeRefConfig.BarcodeFormatKind.QR_CODE) {
            hints.put(EncodeHintType.ERROR_CORRECTION, toZxingLevel(config.errorCorrection()));
            return new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
        }
        // CODE_128: EC ignored (K06b-C4 / BDD-CE-K06b-009); use a readable barcode aspect ratio.
        int width = Math.max(sizePx * 2, sizePx);
        int height = Math.max(sizePx / 3, QrBarcodeRefConfig.MIN_SIZE_PX);
        return new MultiFormatWriter().encode(payload, BarcodeFormat.CODE_128, width, height, hints);
    }

    private static ErrorCorrectionLevel toZxingLevel(QrBarcodeRefConfig.ErrorCorrection level) {
        return switch (level) {
            case L -> ErrorCorrectionLevel.L;
            case M -> ErrorCorrectionLevel.M;
            case Q -> ErrorCorrectionLevel.Q;
            case H -> ErrorCorrectionLevel.H;
        };
    }

    private static DocxAssemblyException encodeFailed(String detail) {
        return new DocxAssemblyException(
                "api.error.rendering.qrBarcodeEncodeFailed",
                "Failed to encode qrBarcodeRef payload: " + detail
        );
    }

    private static DocxAssemblyException encodeFailed(String detail, Throwable cause) {
        return new DocxAssemblyException(
                "RENDERING_FAILED",
                "RENDERING",
                "api.error.rendering.qrBarcodeEncodeFailed",
                "Failed to encode qrBarcodeRef payload: " + detail,
                cause
        );
    }
}
