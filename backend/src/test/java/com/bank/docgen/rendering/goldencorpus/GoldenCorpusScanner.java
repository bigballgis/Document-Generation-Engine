package com.bank.docgen.rendering.goldencorpus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Discovers golden-corpus packages from the classpath resource root and validates structure.
 */
public final class GoldenCorpusScanner {

    private final ObjectMapper objectMapper;
    private final Path overrideRoot;

    public GoldenCorpusScanner(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    public GoldenCorpusScanner(ObjectMapper objectMapper, Path overrideRoot) {
        this.objectMapper = objectMapper;
        this.overrideRoot = overrideRoot;
    }

    public Path resolveRootDirectory() {
        if (overrideRoot != null) {
            return overrideRoot;
        }
        URL resource = Thread.currentThread()
                .getContextClassLoader()
                .getResource(GoldenCorpusThemes.ROOT_RESOURCE);
        if (resource == null) {
            throw new GoldenCorpusException(
                    "Missing classpath resource directory: " + GoldenCorpusThemes.ROOT_RESOURCE
            );
        }
        try {
            URI uri = resource.toURI();
            if (!"file".equalsIgnoreCase(uri.getScheme())) {
                throw new GoldenCorpusException(
                        "golden-corpus root must resolve to a file directory for CE-K07 tests, got: "
                                + uri
                );
            }
            return Paths.get(uri);
        } catch (URISyntaxException ex) {
            throw new GoldenCorpusException("Invalid golden-corpus resource URI", ex);
        }
    }

    public List<GoldenCorpusPackage> scanAndValidate() {
        Path root = resolveRootDirectory();
        if (!Files.isDirectory(root)) {
            throw new GoldenCorpusException("golden-corpus root is not a directory: " + root);
        }

        List<Path> packageDirs = listPackageDirectories(root);
        List<GoldenCorpusPackage> packages = new ArrayList<>();
        for (Path packageDir : packageDirs) {
            packages.add(loadPackage(packageDir));
        }

        validateThemeCoverage(packages);
        for (GoldenCorpusPackage corpusPackage : packages) {
            validatePackageLayout(corpusPackage);
        }
        return packages;
    }

    public GoldenCorpusPackage loadPackage(Path packageDir) {
        Path manifestPath = packageDir.resolve(GoldenCorpusPackageLayout.MANIFEST);
        if (!Files.isRegularFile(manifestPath)) {
            throw new GoldenCorpusException("Missing manifest.json in " + packageDir.getFileName());
        }
        try (InputStream input = Files.newInputStream(manifestPath)) {
            JsonNode root = objectMapper.readTree(input);
            String id = textRequired(root, "id");
            String theme = textOr(root, "theme", id);
            GoldenCorpusMaturity maturity = GoldenCorpusMaturity.fromJson(textRequired(root, "maturity"));
            String title = textOr(root, "title", id);
            String renderMode = textOr(root, "renderMode", "STRUCTURED_ASSEMBLE");
            String pdfSource = textOr(root, "pdfSource", "LIBREOFFICE");
            GoldenCorpusManifest manifest = new GoldenCorpusManifest(
                    id,
                    theme,
                    maturity,
                    title,
                    renderMode,
                    pdfSource
            );
            return new GoldenCorpusPackage(packageDir, manifest);
        } catch (IOException ex) {
            throw new GoldenCorpusException("Failed to read manifest in " + packageDir, ex);
        }
    }

    public void validatePackageLayout(GoldenCorpusPackage corpusPackage) {
        List<String> missing = GoldenCorpusPackageLayout.missingRequiredFiles(corpusPackage.directory());
        if (!missing.isEmpty()) {
            throw new GoldenCorpusException(
                    "Package '" + corpusPackage.id() + "' (" + corpusPackage.maturity()
                            + ") is missing required skeleton files: " + missing
            );
        }
    }

    private void validateThemeCoverage(List<GoldenCorpusPackage> packages) {
        Set<String> foundIds = packages.stream()
                .map(GoldenCorpusPackage::id)
                .collect(Collectors.toCollection(HashSet::new));
        List<String> missingThemes = GoldenCorpusThemes.REQUIRED_THEME_IDS.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
        if (!missingThemes.isEmpty()) {
            throw new GoldenCorpusException(
                    "Missing required golden-corpus themes: " + missingThemes
                            + " (found " + packages.size() + " packages)"
            );
        }
        if (packages.size() < GoldenCorpusThemes.REQUIRED_THEME_IDS.size()) {
            throw new GoldenCorpusException(
                    "golden-corpus must contain at least "
                            + GoldenCorpusThemes.REQUIRED_THEME_IDS.size()
                            + " packages, found " + packages.size()
            );
        }
    }

    private static List<Path> listPackageDirectories(Path root) {
        List<Path> dirs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path child : stream) {
                if (Files.isDirectory(child) && Files.isRegularFile(child.resolve(GoldenCorpusPackageLayout.MANIFEST))) {
                    dirs.add(child);
                }
            }
        } catch (IOException ex) {
            throw new GoldenCorpusException("Failed to list golden-corpus packages under " + root, ex);
        }
        dirs.sort(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)));
        return dirs;
    }

    private static String textRequired(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            throw new GoldenCorpusException("manifest." + field + " is required");
        }
        return node.asText().trim();
    }

    private static String textOr(JsonNode root, String field, String fallback) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || node.asText().isBlank()) {
            return fallback;
        }
        return node.asText().trim();
    }
}
