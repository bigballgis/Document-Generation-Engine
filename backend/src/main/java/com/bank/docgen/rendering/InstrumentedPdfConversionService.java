package com.bank.docgen.rendering;

import java.time.Duration;
import java.time.Instant;

/**
 * Records {@link PdfConversionMetrics} around a delegate {@link PdfConversionService}.
 */
public class InstrumentedPdfConversionService implements PdfConversionService {

    private final PdfConversionService delegate;
    private final PdfConversionMetrics metrics;

    public InstrumentedPdfConversionService(PdfConversionService delegate, PdfConversionMetrics metrics) {
        this.delegate = delegate;
        this.metrics = metrics;
    }

    @Override
    public DocumentArtifactPipeline.PdfConversionResult convertWithResult(
            byte[] docxBytes,
            PdfConversionOptions options
    ) {
        Instant start = Instant.now();
        try {
            DocumentArtifactPipeline.PdfConversionResult result = delegate.convertWithResult(docxBytes, options);
            metrics.record(Duration.between(start, Instant.now()), "success");
            return result;
        } catch (RuntimeException ex) {
            metrics.record(Duration.between(start, Instant.now()), "failure");
            throw ex;
        }
    }
}
