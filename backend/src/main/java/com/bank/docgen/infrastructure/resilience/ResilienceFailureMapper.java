package com.bank.docgen.infrastructure.resilience;

import com.bank.docgen.rendering.PdfConversionCapacityExceededException;
import com.bank.docgen.rendering.RenderingOperationException;
import com.bank.docgen.template.service.TemplateValidationException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.concurrent.TimeoutException;

/**
 * Maps Resilience4j / timeout failures to stable generation domain exceptions (DEF-LRP-D6-001).
 * Must not wrap capacity / validation / rendering failures as {@link TemplateValidationException}.
 */
public final class ResilienceFailureMapper {

    private ResilienceFailureMapper() {
    }

    public static RuntimeException map(Throwable ex) {
        PdfConversionCapacityExceededException capacity = null;
        TemplateValidationException validation = null;
        RenderingOperationException rendering = null;
        GenerationServiceUnavailableException unavailable = null;
        GenerationTimeoutException generationTimeout = null;
        boolean circuitOrBulkhead = false;
        boolean timeout = false;
        RuntimeException firstOtherRuntime = null;

        Throwable current = ex;
        while (current != null) {
            if (capacity == null && current instanceof PdfConversionCapacityExceededException foundCapacity) {
                capacity = foundCapacity;
            } else if (validation == null && current instanceof TemplateValidationException foundValidation) {
                validation = foundValidation;
            } else if (rendering == null && current instanceof RenderingOperationException foundRendering) {
                rendering = foundRendering;
            } else if (unavailable == null && current instanceof GenerationServiceUnavailableException foundUnavailable) {
                unavailable = foundUnavailable;
            } else if (generationTimeout == null && current instanceof GenerationTimeoutException foundTimeout) {
                generationTimeout = foundTimeout;
            } else if (current instanceof CallNotPermittedException || current instanceof BulkheadFullException) {
                circuitOrBulkhead = true;
            } else if (current instanceof TimeoutException) {
                timeout = true;
            } else if (firstOtherRuntime == null && current instanceof RuntimeException runtime) {
                firstOtherRuntime = runtime;
            }
            current = current.getCause();
        }

        if (capacity != null) {
            return capacity;
        }
        if (validation != null) {
            return validation;
        }
        if (rendering != null) {
            return rendering;
        }
        if (unavailable != null) {
            return unavailable;
        }
        if (generationTimeout != null) {
            return generationTimeout;
        }
        if (circuitOrBulkhead) {
            return new GenerationServiceUnavailableException();
        }
        if (timeout) {
            return new GenerationTimeoutException();
        }
        if (firstOtherRuntime != null) {
            return firstOtherRuntime;
        }
        return new GenerationServiceUnavailableException();
    }
}
