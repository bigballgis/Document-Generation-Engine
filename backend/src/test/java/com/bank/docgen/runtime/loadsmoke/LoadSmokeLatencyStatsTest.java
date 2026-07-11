package com.bank.docgen.runtime.loadsmoke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LoadSmokeLatencyStatsTest {

    @Test
    void computesPercentilesErrorRateAndPoolRejections() {
        LoadSmokeLatencyStats stats = new LoadSmokeLatencyStats();
        for (long latency : new long[] {10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L}) {
            stats.recordSuccess(latency);
        }
        stats.recordError(200L, true);
        stats.recordError(300L, false);

        assertThat(stats.sampleCount()).isEqualTo(12);
        assertThat(stats.successCount()).isEqualTo(10);
        assertThat(stats.errorCount()).isEqualTo(2);
        assertThat(stats.poolRejectionCount()).isEqualTo(1);
        assertThat(stats.errorRate()).isEqualTo(2.0d / 12.0d);
        assertThat(stats.percentileMs(0.50d)).isEqualTo(60L);
        assertThat(stats.percentileMs(0.95d)).isEqualTo(300L);
        assertThat(stats.percentileMs(0.99d)).isEqualTo(300L);
        assertThat(stats.summaryLine()).contains("poolRejections=1").contains("p95=");
    }

    @Test
    void rejectsOutOfRangePercentile() {
        LoadSmokeLatencyStats stats = new LoadSmokeLatencyStats();
        stats.recordSuccess(1L);
        assertThatThrownBy(() -> stats.percentileMs(1.5d))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyStatsReturnZeroPercentile() {
        assertThat(new LoadSmokeLatencyStats().percentileMs(0.95d)).isZero();
    }
}
