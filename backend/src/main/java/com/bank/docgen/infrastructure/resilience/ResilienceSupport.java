package com.bank.docgen.infrastructure.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import java.util.function.Supplier;

public final class ResilienceSupport {

    private ResilienceSupport() {
    }

    public static <T> T execute(CircuitBreaker circuitBreaker, Retry retry, Supplier<T> supplier) {
        Supplier<T> decorated = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
        decorated = Retry.decorateSupplier(retry, decorated);
        try {
            return decorated.get();
        } catch (Exception ex) {
            throw ResilienceFailureMapper.map(ex);
        }
    }

    public static void executeVoid(CircuitBreaker circuitBreaker, Retry retry, Runnable action) {
        Runnable decorated = CircuitBreaker.decorateRunnable(circuitBreaker, action);
        decorated = Retry.decorateRunnable(retry, decorated);
        try {
            decorated.run();
        } catch (Exception ex) {
            throw ResilienceFailureMapper.map(ex);
        }
    }
}
