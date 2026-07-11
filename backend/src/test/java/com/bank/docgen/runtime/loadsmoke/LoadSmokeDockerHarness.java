package com.bank.docgen.runtime.loadsmoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates LR-D6 Scenario A (sync generate) and Scenario B (SSE preview) against Docker.
 */
public final class LoadSmokeDockerHarness {

    private final LoadSmokeConfig config;
    private final ObjectMapper mapper;

    public LoadSmokeDockerHarness(LoadSmokeConfig config, ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
    }

    public HarnessResult run() throws Exception {
        LoadSmokeClients clients = new LoadSmokeClients(config, mapper);
        clients.assertBackendHealthy();
        LoadSmokeClients.CredentialBundle credential = clients.loadCredential();
        JsonNode variables = clients.loadVariables();

        Map<String, Object> scenarioA = runScenarioA(clients, credential, variables);
        Map<String, Object> scenarioB = runScenarioB(clients, variables);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("scenarioA", scenarioA);
        summary.put("scenarioB", scenarioB);

        LoadSmokeEvidenceWriter writer = new LoadSmokeEvidenceWriter(mapper);
        Path evidencePath = writer.write(config, summary);

        return new HarnessResult(scenarioA, scenarioB, evidencePath);
    }

    private Map<String, Object> runScenarioA(
            LoadSmokeClients clients,
            LoadSmokeClients.CredentialBundle credential,
            JsonNode variables
    ) throws Exception {
        int concurrency = config.syncConcurrency();
        List<String> formats = LoadSmokeClients.mixedFormats(concurrency);
        LoadSmokeLatencyStats stats = new LoadSmokeLatencyStats();
        Map<String, Integer> errorCodeCounts = new LinkedHashMap<>();
        Map<String, Integer> messageKeyCounts = new LinkedHashMap<>();
        List<String> errorSamples = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        List<Future<LoadSmokeClients.SyncGenerateResult>> futures = new ArrayList<>(concurrency);
        try {
            for (int i = 0; i < concurrency; i++) {
                String format = formats.get(i);
                String key = LoadSmokeClients.newIdempotencyKey("sync-" + i);
                Callable<LoadSmokeClients.SyncGenerateResult> task =
                        () -> clients.syncGenerate(credential, variables, format, key);
                futures.add(pool.submit(task));
            }
            for (Future<LoadSmokeClients.SyncGenerateResult> future : futures) {
                LoadSmokeClients.SyncGenerateResult result = future.get(5, TimeUnit.MINUTES);
                if (result.success()) {
                    stats.recordSuccess(result.latencyMs());
                } else {
                    stats.recordError(result.latencyMs(), result.poolRejection());
                    String code = result.errorCode() == null ? "HTTP_" + result.httpStatus() : result.errorCode();
                    errorCodeCounts.merge(code, 1, Integer::sum);
                    if (result.messageKey() != null && !result.messageKey().isBlank()) {
                        messageKeyCounts.merge(result.messageKey(), 1, Integer::sum);
                    }
                    if (errorSamples.size() < 10) {
                        errorSamples.add(result.format() + " -> " + code
                                + " messageKey=" + result.messageKey()
                                + " http=" + result.httpStatus()
                                + " poolRejection=" + result.poolRejection());
                    }
                }
            }
        } finally {
            pool.shutdownNow();
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("requestedConcurrency", concurrency);
        out.put("formats", formats);
        out.put("sampleCount", stats.sampleCount());
        out.put("successCount", stats.successCount());
        out.put("errorCount", stats.errorCount());
        out.put("errorRate", stats.errorRate());
        out.put("poolRejectionCount", stats.poolRejectionCount());
        out.put("errorCodeCounts", errorCodeCounts);
        out.put("messageKeyCounts", messageKeyCounts);
        out.put("errorSamples", errorSamples);
        out.put("p50Ms", stats.percentileMs(0.50d));
        out.put("p95Ms", stats.percentileMs(0.95d));
        out.put("p99Ms", stats.percentileMs(0.99d));
        out.put("summaryLine", stats.summaryLine());
        out.put(
                "triageNote",
                stats.errorCount() == 0
                        ? "error rate 0"
                        : "Non-zero error rate — named defect DEF-LRP-D6-001"
                                + " (docs/plan/evidence/lrp-d6-load-smoke/TRIAGE-pdf-422.md)."
                                + " Concurrent FOL PDF → TEMPLATE_VALIDATION_FAILED"
                                + " / api.error.generation.serviceUnavailable via ResilienceFailureMapper."
                                + " Do not tune product thresholds to pass."
        );
        return out;
    }

    private Map<String, Object> runScenarioB(
            LoadSmokeClients clients,
            JsonNode variables
    ) throws Exception {
        String token = clients.loginManagement();
        String templateId = clients.resolveTemplateId(token);
        int target = config.sseConcurrency();

        ExecutorService startPool = Executors.newFixedThreadPool(target);
        List<Future<LoadSmokeClients.AsyncPreviewStart>> startFutures = new ArrayList<>(target);
        try {
            for (int i = 0; i < target; i++) {
                startFutures.add(startPool.submit(
                        () -> clients.startAsyncPreview(token, templateId, variables)));
            }
            List<LoadSmokeClients.AsyncPreviewStart> starts = new ArrayList<>();
            List<String> startErrors = new ArrayList<>();
            for (Future<LoadSmokeClients.AsyncPreviewStart> future : startFutures) {
                LoadSmokeClients.AsyncPreviewStart start = future.get(60, TimeUnit.SECONDS);
                if (start.started()) {
                    starts.add(start);
                } else {
                    startErrors.add("HTTP " + start.httpStatus() + " code=" + start.errorCode());
                }
            }

            ExecutorService streamPool = Executors.newFixedThreadPool(Math.max(1, starts.size()));
            try {
                List<Future<LoadSmokeClients.SseTerminalResult>> streamFutures = new ArrayList<>();
                for (LoadSmokeClients.AsyncPreviewStart start : starts) {
                    streamFutures.add(streamPool.submit(() -> clients.readUntilTerminal(
                            token,
                            start.streamUrl(),
                            Duration.ofMinutes(5)
                    )));
                }
                int terminalOk = 0;
                int dropped = 0;
                List<String> terminals = new ArrayList<>();
                for (Future<LoadSmokeClients.SseTerminalResult> future : streamFutures) {
                    LoadSmokeClients.SseTerminalResult result = future.get(6, TimeUnit.MINUTES);
                    if (result.receivedTerminal()) {
                        terminalOk++;
                        terminals.add(result.terminalEvent());
                    } else {
                        dropped++;
                        terminals.add("DROPPED:" + result.detail());
                    }
                }

                Map<String, Object> out = new LinkedHashMap<>();
                out.put("requestedParallelStreams", target);
                out.put("startedStreams", starts.size());
                out.put("terminalReceived", terminalOk);
                out.put("droppedStreams", dropped);
                out.put("terminalEvents", terminals);
                out.put("startErrors", startErrors);
                out.put("zeroDropped", dropped == 0 && !starts.isEmpty());
                out.put("metParallelTarget", starts.size() >= target);
                out.put(
                        "blockerNote",
                        starts.size() < target
                                ? "Could not start " + target
                                + " parallel streams concurrently (default"
                                + " docgen.preview.max-concurrent=3). Restart Docker with"
                                + " SPRING_APPLICATION_JSON"
                                + " containing docgen.preview.max-concurrent>=" + target
                                + " for a valid ≥5 parallel run."
                                : null
                );
                return out;
            } finally {
                streamPool.shutdownNow();
            }
        } finally {
            startPool.shutdownNow();
        }
    }

    public record HarnessResult(
            Map<String, Object> scenarioA,
            Map<String, Object> scenarioB,
            Path evidencePath
    ) {
    }
}
