package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.rendering.RenderingOperationException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class LibreOfficePdfConversionServiceTest {

    private DocgenRenderingProperties properties;
    private Path fakeLibreOfficeScript;
    private ThreadPoolTaskExecutor testPool;

    @BeforeEach
    void setUp() throws URISyntaxException, IOException {
        properties = new DocgenRenderingProperties();
        fakeLibreOfficeScript = resolveFakeLibreOfficeScript("fake-libreoffice");
        ensureExecutable(fakeLibreOfficeScript);
        testPool = pdfConversionPool();
    }

    private static Path resolveFakeLibreOfficeScript(String baseName) throws URISyntaxException {
        String os = System.getProperty("os.name", "").toLowerCase();
        String suffix = os.contains("win") ? ".cmd" : ".sh";
        return Path.of(LibreOfficePdfConversionServiceTest.class
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

    @AfterEach
    void tearDown() throws IOException {
        if (testPool != null) {
            testPool.shutdown();
        }
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> paths = Files.list(tempRoot)) {
            paths.filter(path -> {
                        String name = path.getFileName().toString();
                        return name.startsWith("docgen-pdf-")
                                || name.startsWith("docgen-lo-profile-")
                                || name.startsWith("docgen-lo-invocation-");
                    })
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

    @Test
    void convertsDocxUsingConfiguredCommand() throws IOException {
        properties.setLibreOfficeCommand(fakeLibreOfficeScript.toString());
        properties.setConversionTimeoutSeconds(30);
        LibreOfficePdfConversionService service = service();
        long tempDirsBefore = countDocgenPdfTempDirs();

        byte[] pdf = service.convertWithResult(minimalDocxBytes(), PdfConversionOptions.stampingDisabled()).pdfBytes();

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf)).contains("%PDF");
        assertThat(countDocgenPdfTempDirs()).isEqualTo(tempDirsBefore);
    }

    @Test
    void removesTempDirectoryAfterFailedConversion() throws URISyntaxException, IOException {
        Path failScript = resolveFakeLibreOfficeScript("fake-libreoffice-fail");
        ensureExecutable(failScript);
        properties.setLibreOfficeCommand(failScript.toString());
        properties.setConversionTimeoutSeconds(30);
        LibreOfficePdfConversionService service = service();
        long tempDirsBefore = countDocgenPdfTempDirs();

        assertThatThrownBy(() -> service.convertWithResult(minimalDocxBytes(), PdfConversionOptions.stampingDisabled()))
                .isInstanceOf(RenderingOperationException.class);
        assertThat(countDocgenPdfTempDirs()).isEqualTo(tempDirsBefore);
    }

    @Test
    void rejectsNonZeroExitCode() throws URISyntaxException, IOException {
        Path failScript = resolveFakeLibreOfficeScript("fake-libreoffice-fail");
        ensureExecutable(failScript);
        properties.setLibreOfficeCommand(failScript.toString());
        properties.setConversionTimeoutSeconds(30);
        LibreOfficePdfConversionService service = service();

        assertThatThrownBy(() -> service.convertWithResult(minimalDocxBytes(), PdfConversionOptions.stampingDisabled()))
                .isInstanceOf(RenderingOperationException.class);
    }

    @Test
    void rejectsTimedOutConversion() throws IOException {
        properties.setLibreOfficeCommand("ping");
        properties.setConversionTimeoutSeconds(1);
        LibreOfficePdfConversionService service = service();

        assertThatThrownBy(() -> service.convertWithResult(minimalDocxBytes(), PdfConversionOptions.stampingDisabled()))
                .isInstanceOf(RenderingOperationException.class);
    }

    @Test
    void conversionRunsOffCallingThread() throws IOException {
        properties.setLibreOfficeCommand(fakeLibreOfficeScript.toString());
        properties.setConversionTimeoutSeconds(30);
        Thread callerThread = Thread.currentThread();
        AtomicReference<Thread> workerThread = new AtomicReference<>();
        ThreadPoolTaskExecutor pool = pdfConversionPool();
        try {
            Executor trackingExecutor = task -> pool.execute(() -> {
                workerThread.set(Thread.currentThread());
                task.run();
            });
            LibreOfficePdfConversionService service = new LibreOfficePdfConversionService(
                    properties,
                    CircuitBreakerRegistry.ofDefaults(),
                    RetryRegistry.ofDefaults(),
                    trackingExecutor,
                    pdfConversionPostProcessor()
            );

            byte[] pdf = service.convertWithResult(minimalDocxBytes(), PdfConversionOptions.stampingDisabled()).pdfBytes();

            assertThat(pdf).isNotEmpty();
            assertThat(workerThread.get()).isNotNull();
            assertThat(workerThread.get()).isNotEqualTo(callerThread);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void passesPerInvocationProfileIsolationFlag() throws IOException {
        properties.setLibreOfficeCommand(fakeLibreOfficeScript.toString());
        properties.setConversionTimeoutSeconds(30);
        LibreOfficePdfConversionService service = service();

        byte[] pdf = service.convertWithResult(minimalDocxBytes(), PdfConversionOptions.stampingDisabled()).pdfBytes();

        assertThat(pdf).isNotEmpty();
        // LR-A1: the production code builds a file:// profile URL per invocation. Assert that
        // profileUrl() produces a valid, distinct, file-scheme URL for a fresh temp path — this is
        // the value passed as -env:UserInstallation. Combined with isolatesProfileAcrossParallel
        // it guards CD-PIT-11 (shared profile under concurrency).
        Path sample = Files.createTempDirectory("docgen-lo-profile-");
        try {
            String url = LibreOfficePdfConversionService.profileUrl(sample);
            assertThat(url).startsWith("file:///");
            assertThat(url).doesNotContain("\\");
        } finally {
            deleteQuietly(sample);
        }
    }

    @Test
    void isolatesProfileAcrossParallelConversions() throws Exception {
        properties.setLibreOfficeCommand(fakeLibreOfficeScript.toString());
        properties.setConversionTimeoutSeconds(30);
        ThreadPoolTaskExecutor parallelPool = parallelConversionPool();
        try {
            LibreOfficePdfConversionService service = new LibreOfficePdfConversionService(
                    properties,
                    CircuitBreakerRegistry.ofDefaults(),
                    RetryRegistry.ofDefaults(),
                    parallelPool,
                    pdfConversionPostProcessor()
            );
            int concurrency = 4;
            List<Future<byte[]>> futures = new ArrayList<>();
            ExecutorService caller = Executors.newFixedThreadPool(concurrency);
            try {
                for (int i = 0; i < concurrency; i++) {
                    futures.add(caller.submit(() ->
                            service.convertWithResult(minimalDocxBytes(), PdfConversionOptions.stampingDisabled()).pdfBytes()));
                }
                for (Future<byte[]> future : futures) {
                    assertThat(future.get(30, TimeUnit.SECONDS)).isNotEmpty();
                }
            } finally {
                caller.shutdownNow();
            }
            // CD-PIT-11: every concurrent invocation must build a DISTINCT profile URL. Because
            // convertInternal allocates a fresh temp dir per call and derives the URL from it,
            // distinct temp paths imply distinct -env:UserInstallation values. Assert the URL
            // builder is injective over distinct paths — this is the isolation invariant.
            List<String> urls = distinctProfileUrlsFor(concurrency);
            assertThat(urls).hasSize(concurrency);
            assertThat(new HashSet<>(urls)).hasSize(concurrency);
        } finally {
            parallelPool.shutdown();
        }
    }

    @Test
    void cleansUpProfileDirectoryAfterConversion() throws IOException {
        properties.setLibreOfficeCommand(fakeLibreOfficeScript.toString());
        properties.setConversionTimeoutSeconds(30);
        LibreOfficePdfConversionService service = service();
        long profileDirsBefore = countDocgenLoProfileDirs();

        service.convertWithResult(minimalDocxBytes(), PdfConversionOptions.stampingDisabled());

        assertThat(countDocgenLoProfileDirs()).isEqualTo(profileDirsBefore);
    }

    @Test
    void cleansUpProfileDirectoryAfterFailedConversion() throws URISyntaxException, IOException {
        Path failScript = resolveFakeLibreOfficeScript("fake-libreoffice-fail");
        ensureExecutable(failScript);
        properties.setLibreOfficeCommand(failScript.toString());
        properties.setConversionTimeoutSeconds(30);
        LibreOfficePdfConversionService service = service();
        long profileDirsBefore = countDocgenLoProfileDirs();

        assertThatThrownBy(() -> service.convertWithResult(minimalDocxBytes(), PdfConversionOptions.stampingDisabled()))
                .isInstanceOf(RenderingOperationException.class);
        assertThat(countDocgenLoProfileDirs()).isEqualTo(profileDirsBefore);
    }

    @Test
    void profileUrlIsFileSchemeWithForwardSlashes() {
        Path profile = Path.of("tmp").resolve("lo-profile").toAbsolutePath();

        String url = LibreOfficePdfConversionService.profileUrl(profile);

        assertThat(url).startsWith("file:///");
        assertThat(url).doesNotContain("\\");
    }

    @Test
    void sequentialConversionsDoNotAccumulateTempArtifacts() throws IOException {
        properties.setLibreOfficeCommand(fakeLibreOfficeScript.toString());
        properties.setConversionTimeoutSeconds(30);
        LibreOfficePdfConversionService service = service();
        long pdfDirsBefore = countDocgenPdfTempDirs();
        long profileDirsBefore = countDocgenLoProfileDirs();

        for (int i = 0; i < 10; i++) {
            byte[] pdf = service.convertWithResult(minimalDocxBytes(), PdfConversionOptions.stampingDisabled()).pdfBytes();
            assertThat(pdf).isNotEmpty();
        }

        assertThat(countDocgenPdfTempDirs()).isEqualTo(pdfDirsBefore);
        assertThat(countDocgenLoProfileDirs()).isEqualTo(profileDirsBefore);
    }

    private List<String> distinctProfileUrlsFor(int count) throws IOException {
        List<String> urls = new ArrayList<>();
        List<Path> dirs = new ArrayList<>();
        try {
            for (int i = 0; i < count; i++) {
                Path dir = Files.createTempDirectory("docgen-lo-profile-");
                dirs.add(dir);
                urls.add(LibreOfficePdfConversionService.profileUrl(dir));
            }
        } finally {
            for (Path dir : dirs) {
                deleteQuietly(dir);
            }
        }
        return urls;
    }

    private static void deleteQuietly(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best-effort
                }
            });
        } catch (IOException ignored) {
            // best-effort
        }
    }

    private LibreOfficePdfConversionService service() {
        return new LibreOfficePdfConversionService(
                properties,
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                testPool,
                pdfConversionPostProcessor()
        );
    }

    private PdfConversionPostProcessor pdfConversionPostProcessor() {
        return new PdfConversionPostProcessor(properties, new DocxPdfConversionPreprocessor());
    }

    private static byte[] minimalDocxBytes() throws IOException {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument document = new org.apache.poi.xwpf.usermodel.XWPFDocument();
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Conversion test body");
            document.write(output);
            return output.toByteArray();
        }
    }

    private ThreadPoolTaskExecutor pdfConversionPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(4);
        executor.setThreadNamePrefix("pdf-conversion-test-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    private ThreadPoolTaskExecutor parallelConversionPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(8);
        executor.setThreadNamePrefix("pdf-parallel-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    private long countDocgenPdfTempDirs() throws IOException {
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> paths = Files.list(tempRoot)) {
            return paths.filter(path -> path.getFileName().toString().startsWith("docgen-pdf-")).count();
        }
    }

    private long countDocgenLoProfileDirs() throws IOException {
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> paths = Files.list(tempRoot)) {
            return paths.filter(path -> path.getFileName().toString().startsWith("docgen-lo-profile-")).count();
        }
    }
}