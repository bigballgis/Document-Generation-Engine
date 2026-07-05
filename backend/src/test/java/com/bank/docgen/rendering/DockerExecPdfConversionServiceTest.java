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
import java.util.Comparator;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * Tests the docker-exec conversion path against a fake-docker.sh test double. The fake scripts
 * are bash-only (no .cmd variants), so the test is disabled on Windows: Windows ProcessBuilder
 * cannot launch .sh files directly. The path is exercised on Linux CI; the sibling
 * LibreOfficePdfConversionServiceTest covers the cli mode on both OSes via .cmd/.sh pairs.
 */
@DisabledOnOs(OS.WINDOWS)
class DockerExecPdfConversionServiceTest {

    private DocgenRenderingProperties properties;
    private Path fakeDockerScript;
    private Path fakeLibreOfficeScript;
    private Path fakeDockerState;
    private ThreadPoolTaskExecutor testPool;

    @BeforeEach
    void setUp() throws URISyntaxException, IOException {
        properties = new DocgenRenderingProperties();
        fakeDockerScript = scriptPath("fake-docker.sh");
        fakeLibreOfficeScript = scriptPath("fake-libreoffice.sh");
        fakeDockerState = Files.createTempDirectory("docgen-fake-docker-state-");
        fakeDockerScript.toFile().setExecutable(true);
        fakeLibreOfficeScript.toFile().setExecutable(true);
        properties.setDockerCliCommand(fakeDockerScript.toString());
        properties.setDockerContainerName("docgen-libreoffice-test");
        properties.setLibreOfficeCommand(fakeLibreOfficeScript.toString());
        properties.setConversionTimeoutSeconds(30);
        System.setProperty("DOCGEN_FAKE_DOCKER_STATE", fakeDockerState.toString());
        testPool = pdfConversionPool();
    }

    @AfterEach
    void tearDown() throws IOException {
        System.clearProperty("DOCGEN_FAKE_DOCKER_STATE");
        if (testPool != null) {
            testPool.shutdown();
        }
        if (fakeDockerState != null) {
            try (Stream<Path> paths = Files.walk(fakeDockerState)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // Best-effort cleanup for tests.
                    }
                });
            }
        }
        cleanupHostTempDirs("docgen-docker-pdf-");
    }

    @Test
    void convertsDocxUsingFakeDockerExec() throws IOException {
        DockerExecPdfConversionService service = service();
        long tempDirsBefore = countHostTempDirs("docgen-docker-pdf-");

        byte[] pdf = service.convert(minimalDocxBytes());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf)).contains("%PDF");
        assertThat(countHostTempDirs("docgen-docker-pdf-")).isEqualTo(tempDirsBefore);
    }

    @Test
    void rejectsFailedDockerExec() throws URISyntaxException, IOException {
        properties.setLibreOfficeCommand(scriptPath("fake-libreoffice-fail.sh").toString());
        DockerExecPdfConversionService service = service();

        assertThatThrownBy(() -> service.convert(minimalDocxBytes()))
                .isInstanceOf(TemplateValidationException.class);
    }

    private DockerExecPdfConversionService service() {
        return new DockerExecPdfConversionService(
                properties,
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                testPool,
                new PdfConversionPostProcessor(properties, new DocxPdfConversionPreprocessor())
        );
    }

    private static Path scriptPath(String name) throws URISyntaxException {
        return Path.of(DockerExecPdfConversionServiceTest.class.getResource("/scripts/" + name).toURI());
    }

    private static byte[] minimalDocxBytes() throws IOException {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument();
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Docker exec conversion test");
            document.write(output);
            return output.toByteArray();
        }
    }

    private ThreadPoolTaskExecutor pdfConversionPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(4);
        executor.setThreadNamePrefix("docker-pdf-test-" + UUID.randomUUID() + "-");
        executor.initialize();
        return executor;
    }

    private long countHostTempDirs(String prefix) throws IOException {
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> paths = Files.list(tempRoot)) {
            return paths.filter(path -> path.getFileName().toString().startsWith(prefix)).count();
        }
    }

    private void cleanupHostTempDirs(String prefix) throws IOException {
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
