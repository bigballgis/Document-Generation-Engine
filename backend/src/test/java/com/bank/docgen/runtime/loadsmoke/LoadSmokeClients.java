package com.bank.docgen.runtime.loadsmoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Thin HTTP clients for LR-D6 sync generate + management SSE preview against Docker.
 */
final class LoadSmokeClients {

    static final String POOL_REJECTION_CODE = "PDF_CONVERSION_CAPACITY_EXCEEDED";
    static final String TERMINAL_COMPLETED = "completed";
    static final String TERMINAL_FAILED = "failed";

    private final LoadSmokeConfig config;
    private final ObjectMapper mapper;
    private final HttpClient http;

    LoadSmokeClients(LoadSmokeConfig config, ObjectMapper mapper) {
        this.config = config;
        this.mapper = mapper;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    void assertBackendHealthy() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + "/healthz"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(
                    "Docker acceptance stack not healthy at " + config.baseUrl()
                            + "/healthz (HTTP " + response.statusCode()
                            + "). Deploy via scripts/docker-deploy-queue.ps1 first"
                            + " (FOL seed + preview max-concurrent>=5 for Scenario B)."
            );
        }
    }

    CredentialBundle loadCredential() throws IOException {
        if (!Files.isRegularFile(config.credentialFile())) {
            throw new IOException(
                    "Missing credential file: " + config.credentialFile()
                            + " (run deploy/publish-all-demos.ps1 or FOL publish first;"
                            + " expect .tmp/credentials/CORP-FOL-OFFER.json with"
                            + " externalId + secret)");
        }
        JsonNode root = mapper.readTree(Files.readString(config.credentialFile()));
        String externalId = textOrNull(root, "externalId");
        String secret = textOrNull(root, "secret");
        if (externalId == null || secret == null) {
            throw new IOException("Credential JSON must contain externalId and secret");
        }
        return new CredentialBundle(externalId, secret);
    }

    JsonNode loadVariables() throws IOException {
        if (!Files.isRegularFile(config.variablesFile())) {
            throw new IOException("Missing variables file: " + config.variablesFile());
        }
        String raw = Files.readString(config.variablesFile());
        JsonNode root = mapper.readTree(stripUtf8Bom(raw));
        JsonNode variables = root.get("variables");
        if (variables == null || !variables.isObject()) {
            throw new IOException("Variables JSON must contain a top-level object 'variables'");
        }
        return variables;
    }

    /**
     * PowerShell {@code Set-Content -Encoding UTF8} may prefix a BOM; Jackson rejects it unless stripped.
     */
    static String stripUtf8Bom(String raw) {
        if (raw != null && !raw.isEmpty() && raw.charAt(0) == '\uFEFF') {
            return raw.substring(1);
        }
        return raw;
    }

    SyncGenerateResult syncGenerate(
            CredentialBundle credential,
            JsonNode variables,
            String format,
            String idempotencyKey
    ) throws IOException, InterruptedException {
        ObjectNode body = mapper.createObjectNode();
        ObjectNode output = body.putObject("output");
        output.put("format", format);
        output.put("mode", "SYNC_STREAM");
        body.set("variables", variables);
        body.put("requestId", "lrp-d6-" + idempotencyKey);
        body.put("idempotencyKey", idempotencyKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.runtimeGenerateUrl(config.templateExternalId())))
                .timeout(Duration.ofMinutes(4))
                .header("Content-Type", "application/json")
                .header("X-Api-Credential-Id", credential.externalId())
                .header("X-Api-Credential-Secret", credential.secret())
                .header("X-Access-Account", config.accessAccount())
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        long started = System.nanoTime();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        long latencyMs = (System.nanoTime() - started) / 1_000_000L;
        boolean success = response.statusCode() == 200;
        boolean poolRejection = !success && bodyContainsCode(response.body(), POOL_REJECTION_CODE);
        ErrorFields errorFields = success ? ErrorFields.none() : extractErrorFields(response.body());
        return new SyncGenerateResult(
                response.statusCode(),
                latencyMs,
                success,
                poolRejection,
                errorFields.code(),
                errorFields.messageKey(),
                format,
                response.body() == null ? 0 : response.body().length
        );
    }

    String loginManagement() throws IOException, InterruptedException {
        ObjectNode body = mapper.createObjectNode();
        body.put("username", config.mgmtUsername());
        body.put("password", config.mgmtPassword());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.managementApiBase() + "/auth/login"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Management login failed HTTP " + response.statusCode()
                    + ": " + truncate(response.body()));
        }
        JsonNode token = mapper.readTree(response.body()).path("result").path("accessToken");
        if (token.isMissingNode() || token.asText().isBlank()) {
            throw new IOException("Management login response missing accessToken");
        }
        return token.asText();
    }

    String resolveTemplateId(String accessToken) throws IOException, InterruptedException {
        String path = config.managementApiBase()
                + "/templates?search=" + config.templateExternalId() + "&page=0&size=50";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Template catalog lookup failed HTTP " + response.statusCode()
                    + ": " + truncate(response.body()));
        }
        JsonNode root = mapper.readTree(response.body()).path("result");
        JsonNode items = root.isArray() ? root : root.path("content");
        if (!items.isArray()) {
            throw new IOException("Unexpected template catalog shape");
        }
        for (JsonNode item : items) {
            if (config.templateExternalId().equals(textOrNull(item, "externalId"))) {
                String id = textOrNull(item, "id");
                if (id != null) {
                    return id;
                }
            }
        }
        throw new IOException("Template not found in catalog: " + config.templateExternalId()
                + " (import/publish FOL demo first)");
    }

    AsyncPreviewStart startAsyncPreview(
            String accessToken,
            String templateId,
            JsonNode variables
    ) throws IOException, InterruptedException {
        ObjectNode body = mapper.createObjectNode();
        body.set("variables", variables);
        String url = config.managementApiBase()
                + "/templates/" + templateId + "/previews/async-preview";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 429) {
            return AsyncPreviewStart.rejected(response.statusCode(), extractErrorCode(response.body()));
        }
        if (response.statusCode() != 202 && response.statusCode() != 200) {
            return AsyncPreviewStart.rejected(response.statusCode(), extractErrorCode(response.body()));
        }
        JsonNode result = mapper.readTree(response.body()).path("result");
        String previewId = textOrNull(result, "previewId");
        String streamUrl = textOrNull(result, "streamUrl");
        if (previewId == null || streamUrl == null) {
            throw new IOException("async-preview missing previewId/streamUrl: "
                    + truncate(response.body()));
        }
        return AsyncPreviewStart.started(previewId, streamUrl, response.statusCode());
    }

    SseTerminalResult readUntilTerminal(
            String accessToken,
            String streamUrl,
            Duration timeout
    ) throws IOException, InterruptedException {
        URI uri = streamUrl.startsWith("http")
                ? URI.create(streamUrl)
                : URI.create(config.baseUrl() + streamUrl);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(timeout)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "text/event-stream")
                .GET()
                .build();
        long started = System.nanoTime();
        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            return new SseTerminalResult(
                    false,
                    null,
                    response.statusCode(),
                    (System.nanoTime() - started) / 1_000_000L,
                    "HTTP " + response.statusCode()
            );
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String eventName = null;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("event:")) {
                    eventName = line.substring("event:".length()).trim();
                } else if (line.startsWith("data:") && eventName != null) {
                    if (TERMINAL_COMPLETED.equals(eventName) || TERMINAL_FAILED.equals(eventName)) {
                        return new SseTerminalResult(
                                true,
                                eventName,
                                200,
                                (System.nanoTime() - started) / 1_000_000L,
                                null
                        );
                    }
                    eventName = null;
                } else if (line.isBlank()) {
                    eventName = null;
                }
            }
        }
        return new SseTerminalResult(
                false,
                null,
                200,
                (System.nanoTime() - started) / 1_000_000L,
                "stream ended without terminal event"
        );
    }

    private boolean bodyContainsCode(byte[] body, String code) {
        if (body == null || body.length == 0) {
            return false;
        }
        String text = new String(body, StandardCharsets.UTF_8);
        return text.contains(code);
    }

    private String extractErrorCode(byte[] body) {
        return extractErrorFields(body).code();
    }

    private String extractErrorCode(String body) {
        return extractErrorFields(body).code();
    }

    private ErrorFields extractErrorFields(byte[] body) {
        if (body == null || body.length == 0) {
            return ErrorFields.none();
        }
        return extractErrorFields(new String(body, StandardCharsets.UTF_8));
    }

    private ErrorFields extractErrorFields(String body) {
        if (body == null || body.isBlank()) {
            return ErrorFields.none();
        }
        try {
            JsonNode error = mapper.readTree(body).path("error");
            String code = textOrNull(error, "code");
            String messageKey = textOrNull(error, "messageKey");
            return new ErrorFields(code, messageKey);
        } catch (IOException ignored) {
            return ErrorFields.none();
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return value.asText();
    }

    private static String truncate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 400 ? value : value.substring(0, 400) + "...";
    }

    record ErrorFields(String code, String messageKey) {
        static ErrorFields none() {
            return new ErrorFields(null, null);
        }
    }

    record CredentialBundle(String externalId, String secret) {
    }

    record SyncGenerateResult(
            int httpStatus,
            long latencyMs,
            boolean success,
            boolean poolRejection,
            String errorCode,
            String messageKey,
            String format,
            int bodyBytes
    ) {
    }

    record AsyncPreviewStart(
            boolean started,
            String previewId,
            String streamUrl,
            int httpStatus,
            String errorCode
    ) {
        static AsyncPreviewStart started(String previewId, String streamUrl, int httpStatus) {
            return new AsyncPreviewStart(true, previewId, streamUrl, httpStatus, null);
        }

        static AsyncPreviewStart rejected(int httpStatus, String errorCode) {
            return new AsyncPreviewStart(false, null, null, httpStatus, errorCode);
        }
    }

    record SseTerminalResult(
            boolean receivedTerminal,
            String terminalEvent,
            int httpStatus,
            long latencyMs,
            String detail
    ) {
    }

    static List<String> mixedFormats(int count) {
        List<String> formats = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            formats.add(i % 2 == 0 ? "DOCX" : "PDF");
        }
        return formats;
    }

    static String newIdempotencyKey(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().toLowerCase(Locale.ROOT);
    }
}
