package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.document.PdfArchivalProfile;
import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import java.util.Optional;
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
        // CE-O01 O01-C9: PDFBox finish must not rewrite PDF/A archival bytes.
        if (options != null && options.isPdfA2b()) {
            if (options.pageNumberStampingEnabled()) {
                return PdfPageStampResult.skippedForArchival(pdfBytes);
            }
            return PdfPageStampResult.success(pdfBytes);
        }
        if (!isStampingEnabled(options)) {
            return PdfPageStampResult.success(pdfBytes);
        }
        PdfPageNumberStampPlan plan = options.stampPlan() == null
                ? PdfPageNumberStampPlan.globalOnly()
                : options.stampPlan();
        PdfPageStampResult stamped = PdfPageNumberStamper.stampPageNumbers(pdfBytes, plan);
        if (plan.sectionPaginationUnresolved() && stamped.warning().isEmpty()) {
            return new PdfPageStampResult(
                    stamped.pdfBytes(),
                    Optional.of(FidelityWarningCode.PDF_SECTION_PAGE_NUMBERS_UNRESOLVED)
            );
        }
        return stamped;
    }

    /**
     * When {@code options} is non-null (production path after {@link #resolveOptions}),
     * trust {@code pageNumberStampingEnabled} only — do not OR with the global property.
     * Global applies only when {@code options == null}.
     */
    public boolean isStampingEnabled(PdfConversionOptions options) {
        if (options != null) {
            return options.pageNumberStampingEnabled();
        }
        return renderingProperties.isPdfPageNumberStampingEnabled();
    }

    /**
     * CE-K06c: when a non-null {@link RenderProfile} is present, only
     * {@code pdfPageNumberStampingEnabled} on that profile is authoritative.
     * The global {@code docgen.rendering.pdf-page-number-stamping-enabled} property
     * must not OR/bypass a locked profile true/false. Null profile falls back to global.
     */
    public boolean isStampingEnabled(RenderProfile renderProfile) {
        if (renderProfile != null) {
            return renderProfile.pdfPageNumberStampingEnabled();
        }
        return renderingProperties.isPdfPageNumberStampingEnabled();
    }

    public PdfConversionOptions resolveOptions(byte[] docxBytes, RenderProfile renderProfile) {
        PdfArchivalProfile archival = renderProfile == null
                ? PdfArchivalProfile.NONE
                : renderProfile.pdfArchivalProfile();
        boolean enabled = isStampingEnabled(renderProfile);
        if (!enabled) {
            return PdfConversionOptions.stampingDisabled(archival);
        }
        PdfPageNumberStampPlan plan = DocxPdfPageNumberStampPlanResolver.resolve(docxBytes, renderProfile);
        return PdfConversionOptions.stampingEnabled(plan, archival);
    }
}
