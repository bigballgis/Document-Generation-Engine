package com.bank.docgen.runtime.loadsmoke;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Flag-gated Docker acceptance harness for LR-D6. Never runs under plain {@code mvn verify}.
 *
 * <pre>
 * mvn -B -ntp -f backend/pom.xml -Pdev-fast test -Dtest=LoadSmokeDockerHarnessTest \
 *   -Ddocgen.loadSmoke=true
 * </pre>
 */
@EnabledIfSystemProperty(named = LoadSmokeFlag.PROPERTY, matches = "true")
class LoadSmokeDockerHarnessTest {

    @Test
    void scenarioAAndBRecordBaselinesAgainstDocker() throws Exception {
        LoadSmokeConfig config = LoadSmokeConfig.fromEnvironment();
        assertThat(config.syncConcurrency()).isGreaterThanOrEqualTo(20);
        assertThat(config.sseConcurrency()).isGreaterThanOrEqualTo(5);

        LoadSmokeDockerHarness.HarnessResult result =
                new LoadSmokeDockerHarness(config, new ObjectMapper()).run();

        @SuppressWarnings("unchecked")
        Map<String, Object> scenarioA = result.scenarioA();
        @SuppressWarnings("unchecked")
        Map<String, Object> scenarioB = result.scenarioB();

        assertThat(scenarioA.get("sampleCount")).isEqualTo(config.syncConcurrency());
        assertThat(scenarioA).containsKeys(
                "p95Ms",
                "p99Ms",
                "errorRate",
                "poolRejectionCount",
                "errorCodeCounts",
                "messageKeyCounts",
                "triageNote"
        );

        assertThat(scenarioB).containsKeys(
                "startedStreams",
                "droppedStreams",
                "terminalReceived",
                "metParallelTarget"
        );
        assertThat((Integer) scenarioB.get("droppedStreams")).isZero();
        assertThat(result.evidencePath()).exists();

        // Measurement acceptance (do not tune product thresholds): require ≥5 parallel starts
        // and zero silent SSE drops. If Docker preview max-concurrent < 5, this fails loudly
        // with blockerNote recorded in evidence.
        assertThat((Boolean) scenarioB.get("metParallelTarget"))
                .as("Scenario B needs ≥%d concurrent preview starts; see evidence blockerNote",
                        config.sseConcurrency())
                .isTrue();
    }
}
