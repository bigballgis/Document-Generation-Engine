package com.bank.docgen.infrastructure.resilience;

import com.bank.docgen.template.service.TemplateValidationException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.concurrent.TimeoutException;

public final class ResilienceFailureMapper {

    private ResilienceFailureMapper() {
    }

    public static RuntimeException map(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof CallNotPermittedException || current instanceof TimeoutException) {
                return new TemplateValidationException("api.error.generation.serviceUnavailable");
            }
            if (current instanceof TemplateValidationException validation) {
                return validation;
            }
            if (current instanceof RuntimeException runtime) {
                return runtime;
            }
            current = current.getCause();
        }
        return new TemplateValidationException("api.error.generation.serviceUnavailable");
    }
}
