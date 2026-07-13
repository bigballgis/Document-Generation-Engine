package com.bank.docgen.runtime.loadsmoke;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Aggregates per-request latencies and error outcomes for LR-D6 Scenario A evidence.
 */
public final class LoadSmokeLatencyStats {

    private final List<Long> latenciesMs = new ArrayList<>();
    private int successCount;
    private int errorCount;
    private int poolRejectionCount;

    public synchronized void recordSuccess(long latencyMs) {
        latenciesMs.add(latencyMs);
        successCount++;
    }

    public synchronized void recordError(long latencyMs, boolean poolRejection) {
        latenciesMs.add(latencyMs);
        errorCount++;
        if (poolRejection) {
            poolRejectionCount++;
        }
    }

    public synchronized int sampleCount() {
        return latenciesMs.size();
    }

    public synchronized int successCount() {
        return successCount;
    }

    public synchronized int errorCount() {
        return errorCount;
    }

    public synchronized int poolRejectionCount() {
        return poolRejectionCount;
    }

    public synchronized double errorRate() {
        int total = sampleCount();
        if (total == 0) {
            return 0.0d;
        }
        return (double) errorCount / (double) total;
    }

    public synchronized long percentileMs(double percentile) {
        if (latenciesMs.isEmpty()) {
            return 0L;
        }
        if (percentile < 0.0d || percentile > 1.0d) {
            throw new IllegalArgumentException("percentile must be in [0,1], got " + percentile);
        }
        List<Long> sorted = new ArrayList<>(latenciesMs);
        Collections.sort(sorted);
        if (percentile == 0.0d) {
            return sorted.get(0);
        }
        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        index = Math.max(0, Math.min(index, sorted.size() - 1));
        return sorted.get(index);
    }

    public synchronized String summaryLine() {
        return String.format(
                Locale.ROOT,
                "n=%d success=%d errors=%d errorRate=%.4f poolRejections=%d p50=%dms p95=%dms p99=%dms",
                sampleCount(),
                successCount,
                errorCount,
                errorRate(),
                poolRejectionCount,
                percentileMs(0.50d),
                percentileMs(0.95d),
                percentileMs(0.99d)
        );
    }
}
