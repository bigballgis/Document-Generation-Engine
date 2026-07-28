package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * F4-A3 / FOS-W12-1: DOCX normalization profile isolation — scoped to {@code @TempDir}
 * so Linux CI cannot race on a shared {@code java.io.tmpdir}.
 */
class LibreOfficeDocxNormalizationServiceTest {

    @TempDir
    Path isolatedTempRoot;

    private String previousTmpDir;
    private DocgenRenderingProperties properties;
    private Path fakeLibreOfficeScript;
    private ThreadPoolTaskExecutor testPool;

    @BeforeEach
    void setUp() throws URISyntaxException, IOException {
        previousTmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", isolatedTempRoot.toAbsolutePath().toString());
        properties = new DocgenRenderingProperties();
        fakeLibreOfficeScript = resolveFakeLibreOfficeScript("fake-libreoffice");
        ensureExecutable(fakeLibreOfficeScript);
        properties.setLibreOfficeCommand(fakeLibreOfficeScript.toString());
        properties.setConversionTimeoutSeconds(30);
        testPool = normalizationPool();
    }

    @AfterEach
    void tearDown() {
        if (testPool != null) {
            testPool.shutdown();
        }
        if (previousTmpDir != null) {
            System.setProperty("java.io.tmpdir", previousTmpDir);
        } else {
            System.clearProperty("java.io.tmpdir");
        }
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
                .isInstanceOf(RenderingOperationException.class);
        assertThat(countTempDirs("docgen-lo-norm-profile-")).isEqualTo(profileDirsBefore);
    }

    private LibreOfficeDocxNormalizationService service() {
        return new LibreOfficeDocxNormalizationService(
                properties,
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                testPool,
                new PdfConversionPoolRejectionMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry())
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
        try (Stream<Path> paths = Files.list(isolatedTempRoot)) {
            return paths.filter(path -> path.getFileName().toString().startsWith(prefix)).count();
        }
    }
}
