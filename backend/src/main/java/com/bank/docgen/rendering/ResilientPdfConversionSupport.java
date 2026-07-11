package com.bank.docgen.rendering;

import com.bank.docgen.infrastructure.resilience.ResilienceSupport;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Shared resilience + offload wrapper for LibreOffice-style PDF conversion services.
 */
public final class ResilientPdfConversionSupport {

    private ResilientPdfConversionSupport() {
    }

    public static DocumentArtifactPipeline.PdfConversionResult convertWithResilience(
            CircuitBreaker circuitBreaker,
            Retry retry,
            Executor pdfConversionExecutor,
            int conversionTimeoutSeconds,
            Supplier<DocumentArtifactPipeline.PdfConversionResult> conversion
    ) {
        return convertWithResilience(
                circuitBreaker,
                retry,
                pdfConversionExecutor,
                conversionTimeoutSeconds,
                conversion,
                null
        );
    }

    public static DocumentArtifactPipeline.PdfConversionResult convertWithResilience(
            CircuitBreaker circuitBreaker,
            Retry retry,
            Executor pdfConversionExecutor,
            int conversionTimeoutSeconds,
            Supplier<DocumentArtifactPipeline.PdfConversionResult> conversion,
            Runnable onPoolRejected
    ) {
        return ResilienceSupport.execute(circuitBreaker, retry, () -> PdfConversionOffloadSupport.executeOffloaded(
                pdfConversionExecutor,
                conversionTimeoutSeconds,
                conversion,
                onPoolRejected
        ));
    }
}
