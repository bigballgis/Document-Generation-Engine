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
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
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
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String scriptName = windows ? "/scripts/fake-libreoffice.cmd" : "/scripts/fake-libreoffice.sh";
        fakeLibreOfficeScript = Path.of(
                LibreOfficePdfConversionServiceTest.class.getResource(scriptName).toURI()
        );
        if (!windows) {
            Files.setPosixFilePermissions(fakeLibreOfficeScript, EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
            ));
        }
        testPool = pdfConversionPool();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (testPool != null) {
            testPool.shutdown();
        }
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> paths = Files.list(tempRoot)) {
            paths.filter(path -> path.getFileName().toString().startsWith("docgen-pdf-"))
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

        byte[] pdf = service.convert(minimalDocxBytes());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf)).contains("%PDF");
        assertThat(countDocgenPdfTempDirs()).isEqualTo(tempDirsBefore);
    }

    @Test
    void removesTempDirectoryAfterFailedConversion() throws URISyntaxException, IOException {
        Path failScript = resolveFailScript();
        properties.setLibreOfficeCommand(failScript.toString());
        properties.setConversionTimeoutSeconds(30);
        LibreOfficePdfConversionService service = service();
        long tempDirsBefore = countDocgenPdfTempDirs();

        assertThatThrownBy(() -> service.convert(minimalDocxBytes()))
                .isInstanceOf(TemplateValidationException.class);
        assertThat(countDocgenPdfTempDirs()).isEqualTo(tempDirsBefore);
    }

    @Test
    void rejectsNonZeroExitCode() throws URISyntaxException, IOException {
        Path failScript = resolveFailScript();
        properties.setLibreOfficeCommand(failScript.toString());
        properties.setConversionTimeoutSeconds(30);
        LibreOfficePdfConversionService service = service();

        assertThatThrownBy(() -> service.convert(minimalDocxBytes()))
                .isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void rejectsTimedOutConversion() throws IOException {
        properties.setLibreOfficeCommand("ping");
        properties.setConversionTimeoutSeconds(1);
        LibreOfficePdfConversionService service = service();

        assertThatThrownBy(() -> service.convert(minimalDocxBytes()))
                .isInstanceOf(TemplateValidationException.class);
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

            byte[] pdf = service.convert(minimalDocxBytes());

            assertThat(pdf).isNotEmpty();
            assertThat(workerThread.get()).isNotNull();
            assertThat(workerThread.get()).isNotEqualTo(callerThread);
        } finally {
            pool.shutdown();
        }
    }

    private Path resolveFailScript() throws URISyntaxException, IOException {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String scriptName = windows ? "/scripts/fake-libreoffice-fail.cmd" : "/scripts/fake-libreoffice-fail.sh";
        Path script = Path.of(LibreOfficePdfConversionServiceTest.class.getResource(scriptName).toURI());
        if (!windows) {
            Files.setPosixFilePermissions(script, EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
            ));
        }
        return script;
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

    private long countDocgenPdfTempDirs() throws IOException {
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> paths = Files.list(tempRoot)) {
            return paths.filter(path -> path.getFileName().toString().startsWith("docgen-pdf-")).count();
        }
    }
}
