package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.template.service.TemplateValidationException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * F4-A3 / BDD-F4-A3-003: DOCX normalization profile isolation and cleanup regression.
 */
class LibreOfficeDocxNormalizationServiceTest {

    private DocgenRenderingProperties properties;
    private Path fakeLibreOfficeScript;
    private ThreadPoolTaskExecutor testPool;

    @BeforeEach
    void setUp() throws URISyntaxException, IOException {
        properties = new DocgenRenderingProperties();
        fakeLibreOfficeScript = resolveFakeLibreOfficeScript("fake-libreoffice");
        ensureExecutable(fakeLibreOfficeScript);
        properties.setLibreOfficeCommand(fakeLibreOfficeScript.toString());
        properties.setConversionTimeoutSeconds(30);
        testPool = normalizationPool();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (testPool != null) {
            testPool.shutdown();
        }
        cleanupTempDirs("docgen-docx-normalize-");
        cleanupTempDirs("docgen-lo-norm-profile-");
    }

    @Test
    void normalizesDocxUsingConfiguredCommand() throws IOException {
        LibreOfficeDocxNormalizationService service = service();
        long profileDirsBefore = countTempDirs("docgen-lo-norm-profile-");

        byte[] normalized = service.normalize(minimalDocxBytes());

        assertThat(normalized).isNotEmpty();
        assertThat(countTempDirs("docgen-lo-norm-profile-")).isEqualTo(profileDirsBefore);
    }

    @Test
    void cleansUpProfileDirectoryAfterFailedNormalization() throws URISyntaxException, IOException {
        Path failScript = resolveFakeLibreOfficeScript("fake-libreoffice-fail");
        ensureExecutable(failScript);
        properties.setLibreOfficeCommand(failScript.toString());
        LibreOfficeDocxNormalizationService service = service();
        long profileDirsBefore = countTempDirs("docgen-lo-norm-profile-");

        assertThatThrownBy(() -> service.normalize(minimalDocxBytes()))
                .isInstanceOf(TemplateValidationException.class);
        assertThat(countTempDirs("docgen-lo-norm-profile-")).isEqualTo(profileDirsBefore);
    }

    private LibreOfficeDocxNormalizationService service() {
        return new LibreOfficeDocxNormalizationService(
                properties,
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                testPool
        );
    }

    private static Path resolveFakeLibreOfficeScript(String baseName) throws URISyntaxException {
        String os = System.getProperty("os.name", "").toLowerCase();
        String suffix = os.contains("win") ? ".cmd" : ".sh";
        return Path.of(LibreOfficeDocxNormalizationServiceTest.class
                .getResource("/scripts/" + baseName + suffix)
                .toURI());
    }

    private static void ensureExecutable(Path script) throws IOException {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            Files.setPosixFilePermissions(script, EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
            ));
        }
    }

    private static byte[] minimalDocxBytes() throws IOException {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument();
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Normalization test body");
            document.write(output);
            return output.toByteArray();
        }
    }

    private ThreadPoolTaskExecutor normalizationPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(4);
        executor.setThreadNamePrefix("docx-normalize-test-");
        executor.initialize();
        return executor;
    }

    private long countTempDirs(String prefix) throws IOException {
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> paths = Files.list(tempRoot)) {
            return paths.filter(path -> path.getFileName().toString().startsWith(prefix)).count();
        }
    }

    private void cleanupTempDirs(String prefix) throws IOException {
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> paths = Files.list(tempRoot)) {
            paths.filter(path -> path.getFileName().toString().startsWith(prefix))
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try (Stream<Path> children = Files.walk(path)) {
                            children.sorted(Comparator.reverseOrder()).forEach(child -> {
                                try {
                                    Files.deleteIfExists(child);
                                } catch (IOException ignored) {
                                    // Best-effort cleanup for tests.
                                }
                            });
                        } catch (IOException ignored) {
                            // Best-effort cleanup for tests.
                        }
                    });
        }
    }
}
