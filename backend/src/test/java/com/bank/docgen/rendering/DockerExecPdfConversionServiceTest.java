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
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Docker-exec conversion path against OS-native fake-docker doubles (FOS-W12-1).
 * Temp roots are {@code @TempDir}-scoped so parallel Surefire cannot race on shared tmp.
 */
class DockerExecPdfConversionServiceTest {

    @TempDir
    Path isolatedTempRoot;

    private String previousTmpDir;
    private DocgenRenderingProperties properties;
    private Path fakeDockerScript;
    private Path fakeLibreOfficeScript;
    private Path fakeDockerState;
    private ThreadPoolTaskExecutor testPool;

    @BeforeEach
    void setUp() throws URISyntaxException, IOException {
        previousTmpDir = System.getProperty("java.io.tmpdir");
        System.setProperty("java.io.tmpdir", isolatedTempRoot.toAbsolutePath().toString());
        properties = new DocgenRenderingProperties();
        fakeDockerScript = resolveScript("fake-docker");
        fakeLibreOfficeScript = resolveScript("fake-libreoffice");
        fakeDockerState = Files.createTempDirectory(isolatedTempRoot, "docgen-fake-docker-state-");
        ensureExecutable(fakeDockerScript);
        ensureExecutable(fakeLibreOfficeScript);
        properties.setDockerCliCommand(fakeDockerScript.toString());
        properties.setDockerContainerName("docgen-libreoffice-test");
        properties.setLibreOfficeCommand(fakeLibreOfficeScript.toString());
        properties.setConversionTimeoutSeconds(30);
        System.setProperty("DOCGEN_FAKE_DOCKER_STATE", fakeDockerState.toString());
        // Bash double reads TMPDIR/env, not Java system properties — pin pointer where the child looks.
        String processTmp = System.getenv().getOrDefault("TMPDIR", "/tmp");
        Path pointer = Path.of(processTmp, "docgen-fake-docker-state.pointer");
        Files.writeString(pointer, fakeDockerState.toString());
        Files.writeString(isolatedTempRoot.resolve("docgen-fake-docker-state.pointer"), fakeDockerState.toString());
        testPool = pdfConversionPool();
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("DOCGEN_FAKE_DOCKER_STATE");
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
        Path fail = resolveScript("fake-libreoffice-fail");
        ensureExecutable(fail);
        properties.setLibreOfficeCommand(fail.toString());
        DockerExecPdfConversionService service = service();

        assertThatThrownBy(() -> service.convert(minimalDocxBytes()))
                .isInstanceOf(RenderingOperationException.class);
    }

    @Test
    void usesDistinctContainerProfilesAcrossParallelConversions() throws Exception {
        ThreadPoolTaskExecutor parallelPool = parallelConversionPool();
        try {
            DockerExecPdfConversionService parallelService = new DockerExecPdfConversionService(
                    properties,
                    CircuitBreakerRegistry.ofDefaults(),
                    RetryRegistry.ofDefaults(),
                    parallelPool,
                    new PdfConversionPostProcessor(properties, new DocxPdfConversionPreprocessor()),
                    new PdfConversionPoolRejectionMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry())
            );
            int concurrency = 2;
            List<Future<byte[]>> futures = new ArrayList<>();
            ExecutorService callers = Executors.newFixedThreadPool(concurrency);
            try {
                for (int i = 0; i < concurrency; i++) {
                    futures.add(callers.submit(() -> parallelService.convert(minimalDocxBytes())));
                }
                for (Future<byte[]> future : futures) {
                    assertThat(future.get(30, TimeUnit.SECONDS)).isNotEmpty();
                }
            } finally {
                callers.shutdownNow();
            }

            Set<String> invokedProfiles = readInvokedProfiles();
            assertThat(invokedProfiles).hasSize(concurrency);
            assertThat(countContainerProfileDirs())
                    .as("container profile directories must be cleaned after conversion")
                    .isZero();
        } finally {
            parallelPool.shutdown();
        }
    }

    @Test
    void cleansUpContainerProfileAfterConversion() throws IOException {
        DockerExecPdfConversionService service = service();
        long profilesBefore = countContainerProfileDirs();

        service.convert(minimalDocxBytes());

        assertThat(countContainerProfileDirs()).isEqualTo(profilesBefore);
    }

    private DockerExecPdfConversionService service() {
        return new DockerExecPdfConversionService(
                properties,
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                testPool,
                new PdfConversionPostProcessor(properties, new DocxPdfConversionPreprocessor()),
                new PdfConversionPoolRejectionMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry())
        );
    }

    private static Path resolveScript(String baseName) throws URISyntaxException {
        String os = System.getProperty("os.name", "").toLowerCase();
        String suffix = os.contains("win") ? ".cmd" : ".sh";
        return Path.of(DockerExecPdfConversionServiceTest.class
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

    private ThreadPoolTaskExecutor parallelConversionPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(4);
        executor.setThreadNamePrefix("docker-pdf-parallel-" + UUID.randomUUID() + "-");
        executor.initialize();
        return executor;
    }

    private Set<String> readInvokedProfiles() throws IOException {
        Path log = fakeDockerState.resolve("profile-invocations.log");
        if (!Files.exists(log)) {
            return Set.of();
        }
        return Set.copyOf(Files.readAllLines(log));
    }

    private long countContainerProfileDirs() throws IOException {
        Path profilesRoot = fakeDockerState.resolve(properties.getDockerContainerName()).resolve("tmp");
        if (!Files.exists(profilesRoot)) {
            return 0;
        }
        try (Stream<Path> paths = Files.list(profilesRoot)) {
            return paths.filter(path -> path.getFileName().toString().startsWith("docgen-lo-profile-")).count();
        }
    }

    private long countHostTempDirs(String prefix) throws IOException {
        try (Stream<Path> paths = Files.list(isolatedTempRoot)) {
            return paths.filter(path -> path.getFileName().toString().startsWith(prefix)).count();
        }
    }
}
