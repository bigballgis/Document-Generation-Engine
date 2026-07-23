package com.bank.docgen.demo.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Keep-set demo package contract support — screenshot bank-letter Live set (TM #164).
 */
public final class DemoPackageContractSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** Seven deploy packages covering eight keep external IDs. */
    private static final List<String> PACKAGE_CODES = List.of(
            "demo-fol",
            "demo-credit-limit",
            "demo-annual-review",
            "demo-facility-amendment",
            "demo-commitment",
            "demo-formal-demand",
            "demo-covenant-waiver"
    );

    private DemoPackageContractSupport() {
    }

    public static List<String> packageCodes() {
        return PACKAGE_CODES;
    }

    public static Path deployRoot() {
        return Path.of("..", "deploy").normalize();
    }

    public static Path packageRoot(String packageCode) {
        return deployRoot().resolve(packageCode);
    }

    public static void assertPackageStructure(String packageCode) throws IOException {
        Path root = packageRoot(packageCode);
        assertDirectoryExists(root.resolve("assets"));
        assertDirectoryExists(root.resolve("config"));
        assertDirectoryExists(root.resolve("sql"));

        String shortCode = packageCode.replace("demo-", "");
        Path importScript = root.resolve("import-" + shortCode + "-demo.ps1");
        if (!Files.exists(importScript)) {
            throw new IllegalStateException("Missing import script: " + importScript);
        }

        Path configDir = root.resolve("config");
        try (Stream<Path> configs = Files.list(configDir)) {
            boolean hasTemplateConfig = configs.anyMatch(path -> path.getFileName().toString().endsWith("-template-config.json"));
            if (!hasTemplateConfig) {
                throw new IllegalStateException("Missing template-config.json in " + configDir);
            }
        }

        List<String> requiredSuffixes = List.of(
                "-catalog-manifest.json",
                "-variables.json",
                "-binding-overlays.json",
                "-demo-test-variables.json"
        );
        for (String suffix : requiredSuffixes) {
            try (Stream<Path> configs = Files.list(configDir)) {
                boolean found = configs.anyMatch(path -> path.getFileName().toString().endsWith(suffix));
                if (!found) {
                    throw new IllegalStateException("Missing *" + suffix + " in " + configDir);
                }
            }
        }

        try (Stream<Path> sqlFiles = Files.list(root.resolve("sql"))) {
            boolean hasSql = sqlFiles.anyMatch(path -> path.getFileName().toString().endsWith(".sql"));
            if (!hasSql) {
                throw new IllegalStateException("Missing SQL seed in " + root.resolve("sql"));
            }
        }
    }

    public static JsonNode readJson(Path path) throws IOException {
        String raw = Files.readString(path);
        if (!raw.isEmpty() && raw.charAt(0) == '\uFEFF') {
            raw = raw.substring(1);
        }
        return OBJECT_MAPPER.readTree(raw);
    }

    public static JsonNode templateConfig(String packageCode) throws IOException {
        Path configDir = packageRoot(packageCode).resolve("config");
        try (Stream<Path> configs = Files.list(configDir)) {
            Path configPath = configs
                    .filter(path -> path.getFileName().toString().endsWith("-template-config.json"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No template-config in " + packageCode));
            return readJson(configPath);
        }
    }

    public static String catalogMarker(String packageCode) throws IOException {
        return templateConfig(packageCode).path("catalogMarker").asText("");
    }

    private static void assertDirectoryExists(Path path) {
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException("Missing directory: " + path);
        }
    }
}
