package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.RenderProfile;
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

    public byte[] prepareDocxForConversion(byte[] docxBytes, PdfConversionOptions options) {
        if (!isStampingEnabled(options)) {
            return docxBytes;
        }
        return pdfConversionPreprocessor.prepareForPdfConversion(docxBytes);
    }

    public PdfPageStampResult finishPdf(byte[] pdfBytes, PdfConversionOptions options) {
        if (!isStampingEnabled(options)) {
            return PdfPageStampResult.success(pdfBytes);
        }
        PdfPageNumberStampPlan plan = options.stampPlan() == null
                ? PdfPageNumberStampPlan.globalOnly()
                : options.stampPlan();
        return PdfPageNumberStamper.stampPageNumbers(pdfBytes, plan);
    }

    public boolean isStampingEnabled(PdfConversionOptions options) {
        if (options != null && options.pageNumberStampingEnabled()) {
            return true;
        }
        return renderingProperties.isPdfPageNumberStampingEnabled();
    }

    public boolean isStampingEnabled(RenderProfile renderProfile) {
        if (renderProfile != null && renderProfile.pdfPageNumberStampingEnabled()) {
            return true;
        }
        return renderingProperties.isPdfPageNumberStampingEnabled();
    }

    public PdfConversionOptions resolveOptions(byte[] docxBytes, RenderProfile renderProfile) {
        boolean enabled = isStampingEnabled(renderProfile);
        if (!enabled) {
            return PdfConversionOptions.stampingDisabled();
        }
        PdfPageNumberStampPlan plan = DocxPdfPageNumberStampPlanResolver.resolve(docxBytes, renderProfile);
        return PdfConversionOptions.stampingEnabled(plan);
    }
}
