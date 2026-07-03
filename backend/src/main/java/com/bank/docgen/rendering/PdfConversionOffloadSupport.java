package com.bank.docgen.rendering;

import com.bank.docgen.template.service.TemplateValidationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public final class PdfConversionOffloadSupport {

    private static final int TIMEOUT_BUFFER_SECONDS = 5;

    private PdfConversionOffloadSupport() {
    }

    public static <T> T executeOffloaded(
            Executor executor,
            int conversionTimeoutSeconds,
            Supplier<T> conversion
    ) {
        Future<T> future;
        try {
            future = CompletableFuture.supplyAsync(conversion, executor);
        } catch (RejectedExecutionException ex) {
            throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
        }
        try {
            return future.get(conversionTimeoutSeconds + TIMEOUT_BUFFER_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof TemplateValidationException validationException) {
                throw validationException;
            }
            if (cause instanceof RejectedExecutionException) {
                throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
            }
            throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
        }
    }
}
