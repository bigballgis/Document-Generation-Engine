package com.bank.docgen.rendering.service;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * In-process semaphore limiting concurrent preview generation requests.
 * For single-instance deployments this provides the required concurrency cap.
 */
@Component
public class PreviewConcurrencyGuard {

    private final int maxConcurrent;
    private final AtomicInteger activeCount = new AtomicInteger(0);

    public PreviewConcurrencyGuard(
            @Value("${docgen.preview.max-concurrent:3}") int maxConcurrent
    ) {
        this.maxConcurrent = maxConcurrent;
    }

    /**
     * Attempts to acquire a slot. Returns {@code true} if successful, {@code false} if limit reached.
     */
    public boolean tryAcquire() {
        int current;
        do {
            current = activeCount.get();
            if (current >= maxConcurrent) {
                return false;
            }
        } while (!activeCount.compareAndSet(current, current + 1));
        return true;
    }

    public void release() {
        activeCount.decrementAndGet();
    }

    public int getActiveCount() {
        return activeCount.get();
    }

    public int getMaxConcurrent() {
        return maxConcurrent;
    }
}
