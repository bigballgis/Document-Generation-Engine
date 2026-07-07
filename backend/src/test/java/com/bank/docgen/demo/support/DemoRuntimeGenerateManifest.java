package com.bank.docgen.demo.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Mirrors {@code deploy/demo-shared/demo-runtime-generate-manifest.json} and
 * {@code deploy/generate-all-demos.ps1} for P23-T14 contract tests.
 */
public final class DemoRuntimeGenerateManifest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private DemoRuntimeGenerateManifest() {
    }

    public static Path manifestPath() {
        return DemoPackageContractSupport.deployRoot().resolve("demo-shared/demo-runtime-generate-manifest.json");
    }

    public static JsonNode load() throws IOException {
        return OBJECT_MAPPER.readTree(Files.readString(manifestPath()));
    }

    public static List<String> templateExternalIds(JsonNode manifest) {
        List<String> ids = new ArrayList<>();
        for (JsonNode entry : manifest.path("templates")) {
            ids.add(entry.path("externalId").asText());
        }
        return List.copyOf(ids);
    }

    public static Path variablesFixturePath(JsonNode templateEntry) {
        return Path.of("..").resolve(templateEntry.path("variablesFixture").asText()).normalize();
    }

    public static boolean fixtureExists(JsonNode templateEntry) throws IOException {
        return Files.exists(variablesFixturePath(templateEntry));
    }
}
