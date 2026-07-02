package com.bank.docgen.rendering;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import org.springframework.stereotype.Component;

/**
 * Optional PDF page-number finishing. Disabled by default until final PDF fidelity work lands.
 */
@Component
public class PdfConversionPostProcessor {

    private final DocgenRenderingProperties renderingProperties;
    private final DocxPdfConversionPreprocessor pdfConversionPreprocessor;

    public PdfConversionPostProcessor(
            DocgenRenderingProperties renderingProperties,
            DocxPdfConversionPreprocessor pdfConversionPreprocessor
    ) {
        this.renderingProperties = renderingProperties;
        this.pdfConversionPreprocessor = pdfConversionPreprocessor;
    }

    public byte[] prepareDocxForConversion(byte[] docxBytes) {
        if (!renderingProperties.isPdfPageNumberStampingEnabled()) {
            return docxBytes;
        }
        return pdfConversionPreprocessor.prepareForPdfConversion(docxBytes);
    }

    public byte[] finishPdf(byte[] pdfBytes) {
        if (!renderingProperties.isPdfPageNumberStampingEnabled()) {
            return pdfBytes;
        }
        return PdfPageNumberStamper.stampPageNumbers(pdfBytes);
    }
}
