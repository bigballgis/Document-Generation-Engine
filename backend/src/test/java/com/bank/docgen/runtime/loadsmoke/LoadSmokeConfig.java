package com.bank.docgen.runtime.loadsmoke;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Resolves LR-D6 harness configuration from system properties and environment variables.
 */
public final class LoadSmokeConfig {

    public static final String DEFAULT_BASE_URL = "http://localhost:8080";
    public static final String DEFAULT_ENVIRONMENT = "dev";
    public static final String DEFAULT_TEMPLATE_EXTERNAL_ID = "CORP-FOL-OFFER";
    public static final String DEFAULT_MGMT_USER = "10000003";
    public static final String DEFAULT_MGMT_PASSWORD = "ChangeMe123!";
    /** Must be mapped to CORP_API (FOL) via seed yml or load-smoke compose override. */
    public static final String DEFAULT_ACCESS_ACCOUNT = "lrp-d6-load-smoke";
    public static final int DEFAULT_SYNC_CONCURRENCY = 20;
    public static final int DEFAULT_SSE_CONCURRENCY = 5;

    private final String baseUrl;
    private final String environment;
    private final String templateExternalId;
    private final Path credentialFile;
    private final Path variablesFile;
    private final Path evidenceDir;
    private final String mgmtUsername;
    private final String mgmtPassword;
    private final String accessAccount;
    private final int syncConcurrency;
    private final int sseConcurrency;
    private final String hardwareNote;
    private final String stackVersion;

    private LoadSmokeConfig(
            String baseUrl,
            String environment,
            String templateExternalId,
            Path credentialFile,
            Path variablesFile,
            Path evidenceDir,
            String mgmtUsername,
            String mgmtPassword,
            String accessAccount,
            int syncConcurrency,
            int sseConcurrency,
            String hardwareNote,
            String stackVersion
    ) {
        this.baseUrl = baseUrl;
        this.environment = environment;
        this.templateExternalId = templateExternalId;
        this.credentialFile = credentialFile;
        this.variablesFile = variablesFile;
        this.evidenceDir = evidenceDir;
        this.mgmtUsername = mgmtUsername;
        this.mgmtPassword = mgmtPassword;
        this.accessAccount = accessAccount;
        this.syncConcurrency = syncConcurrency;
        this.sseConcurrency = sseConcurrency;
        this.hardwareNote = hardwareNote;
        this.stackVersion = stackVersion;
    }

    public static LoadSmokeConfig fromEnvironment() {
        Path repoRoot = resolveRepoRoot();
        return new LoadSmokeConfig(
                firstNonBlank(
                        propOrEnv("docgen.loadSmoke.baseUrl", "DOCGEN_LOAD_SMOKE_BASE_URL"),
                        DEFAULT_BASE_URL),
                firstNonBlank(
                        propOrEnv("docgen.loadSmoke.environment", "DOCGEN_LOAD_SMOKE_ENVIRONMENT"),
                        DEFAULT_ENVIRONMENT),
                firstNonBlank(
                        propOrEnv(
                                "docgen.loadSmoke.templateExternalId",
                                "DOCGEN_LOAD_SMOKE_TEMPLATE_EXTERNAL_ID"),
                        DEFAULT_TEMPLATE_EXTERNAL_ID),
                Paths.get(firstNonBlank(
                        propOrEnv(
                                "docgen.loadSmoke.credentialFile",
                                "DOCGEN_LOAD_SMOKE_CREDENTIAL_FILE"),
                        repoRoot.resolve(".tmp/credentials/CORP-FOL-OFFER.json").toString())),
                Paths.get(firstNonBlank(
                        propOrEnv(
                                "docgen.loadSmoke.variablesFile",
                                "DOCGEN_LOAD_SMOKE_VARIABLES_FILE"),
                        repoRoot.resolve("deploy/demo-fol/config/fol-demo-test-variables.json")
                                .toString())),
                Paths.get(firstNonBlank(
                        propOrEnv("docgen.loadSmoke.evidenceDir", "DOCGEN_LOAD_SMOKE_EVIDENCE_DIR"),
                        repoRoot.resolve("docs/plan/evidence/lrp-d6-load-smoke").toString())),
                firstNonBlank(
                        propOrEnv("docgen.loadSmoke.mgmtUser", "DOCGEN_LOAD_SMOKE_MGMT_USER"),
                        DEFAULT_MGMT_USER),
                firstNonBlank(
                        propOrEnv("docgen.loadSmoke.mgmtPassword", "DOCGEN_LOAD_SMOKE_MGMT_PASSWORD"),
                        DEFAULT_MGMT_PASSWORD),
                firstNonBlank(
                        propOrEnv(
                                "docgen.loadSmoke.accessAccount",
                                "DOCGEN_LOAD_SMOKE_ACCESS_ACCOUNT"),
                        DEFAULT_ACCESS_ACCOUNT),
                parsePositiveInt(
                        propOrEnv(
                                "docgen.loadSmoke.syncConcurrency",
                                "DOCGEN_LOAD_SMOKE_SYNC_CONCURRENCY"),
                        DEFAULT_SYNC_CONCURRENCY),
                parsePositiveInt(
                        propOrEnv(
                                "docgen.loadSmoke.sseConcurrency",
                                "DOCGEN_LOAD_SMOKE_SSE_CONCURRENCY"),
                        DEFAULT_SSE_CONCURRENCY),
                firstNonBlank(
                        propOrEnv("docgen.loadSmoke.hardwareNote", "DOCGEN_LOAD_SMOKE_HARDWARE_NOTE"),
                        "<hardware-note-placeholder>"),
                firstNonBlank(
                        propOrEnv("docgen.loadSmoke.stackVersion", "DOCGEN_LOAD_SMOKE_STACK_VERSION"),
                        "<stack-version-placeholder>")
        );
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String environment() {
        return environment;
    }

    public String templateExternalId() {
        return templateExternalId;
    }

    public Path credentialFile() {
        return credentialFile;
    }

    public Path variablesFile() {
        return variablesFile;
    }

    public Path evidenceDir() {
        return evidenceDir;
    }

    public String mgmtUsername() {
        return mgmtUsername;
    }

    public String mgmtPassword() {
        return mgmtPassword;
    }

    public String accessAccount() {
        return accessAccount;
    }

    public int syncConcurrency() {
        return syncConcurrency;
    }

    public int sseConcurrency() {
        return sseConcurrency;
    }

    public String hardwareNote() {
        return hardwareNote;
    }

    public String stackVersion() {
        return stackVersion;
    }

    public String runtimeGenerateUrl(String externalId) {
        return baseUrl + "/api/" + environment + "/v1/templates/" + externalId + "/default/generate";
    }

    public String managementApiBase() {
        return baseUrl + "/api/management/v1";
    }

    private static Path resolveRepoRoot() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        if (cwd.getFileName() != null && "backend".equals(cwd.getFileName().toString())) {
            return cwd.getParent();
        }
        Path marker = cwd.resolve("backend/pom.xml");
        if (marker.toFile().isFile()) {
            return cwd;
        }
        return cwd;
    }

    private static String propOrEnv(String property, String env) {
        String fromProp = System.getProperty(property);
        if (fromProp != null && !fromProp.isBlank()) {
            return fromProp.trim();
        }
        String fromEnv = System.getenv(env);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        return null;
    }

    private static String firstNonBlank(String primary, String fallback) {
        return Optional.ofNullable(primary).filter(value -> !value.isBlank()).orElse(fallback);
    }

    private static int parsePositiveInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
