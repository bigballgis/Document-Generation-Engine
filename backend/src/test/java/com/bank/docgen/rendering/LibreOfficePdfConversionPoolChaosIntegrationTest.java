package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * IBL-D4 — LO-facing chaos halves.
 *
 * <ul>
 *   <li>Process hang timeout via fake hang script (default verify — no real soffice).</li>
 *   <li>Real {@code soffice} saturation reject under tiny pool ({@code @Tag("libreoffice")};
 *       optional skip / {@code -Plibreoffice-ci} fail-closed).</li>
 * </ul>
 */
@Tag(PdfConversionPoolChaosTest.TAG)
class LibreOfficePdfConversionPoolChaosIntegrationTest {

    private ThreadPoolTaskExecutor pool;
    private MeterRegistry registry;

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.shutdown();
            pool = null;
        }
        registry = null;
    }

    @Test
    void processHangTimeoutFailsClosedWithoutRejectionMetric() throws Exception {
        Path hangScript = resolveHangScript();
        ensureExecutable(hangScript);

        registry = new SimpleMeterRegistry();
        pool = boundedPool(1, 0);
        new PdfConversionPoolMetrics(pool).bindTo(registry);
        PdfConversionPoolRejectionMetrics rejectionMetrics = new PdfConversionPoolRejectionMetrics(registry);

        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setLibreOfficeCommand(hangScript.toString());
        properties.setConversionTimeoutSeconds(1);

        LibreOfficePdfConversionService service = new LibreOfficePdfConversionService(
                properties,
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                pool,
                new PdfConversionPostProcessor(properties, new DocxPdfConversionPreprocessor()),
                rejectionMetrics
        );

        assertThatThrownBy(() -> service.convertWithResult(
                minimalDocxBytes(),
                PdfConversionOptions.stampingDisabled()
        ))
                .isInstanceOf(RenderingOperationException.class)
                .hasMessage("api.error.generation.pdfConversionFailed");

        assertThat(registry.find("docgen.pdf.conversion.pool.rejections").counter().count()).isZero();
    }

    @Test
    @Tag(LibreOfficeTestSupport.TAG)
    void realSofficePathRejectsUnderSaturationThenRecovers() throws Exception {
        LibreOfficeTestSupport.requireSoffice(
                "LibreOfficePdfConversionPoolChaosIntegrationTest / IBL-D4 saturation"
        );

        registry = new SimpleMeterRegistry();
        pool = boundedPool(1, 0);
        new PdfConversionPoolMetrics(pool).bindTo(registry);
        PdfConversionPoolRejectionMetrics rejectionMetrics = new PdfConversionPoolRejectionMetrics(registry);

        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setLibreOfficeCommand(LibreOfficeTestSupport.sofficeCommand());
        properties.setConversionPoolSize(1);
        properties.setConversionQueueCapacity(0);
        properties.setConversionTimeoutSeconds(120);

        LibreOfficePdfConversionService service = new LibreOfficePdfConversionService(
                properties,
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                pool,
                new PdfConversionPostProcessor(properties, new DocxPdfConversionPreprocessor()),
                rejectionMetrics
        );

        Semaphore hold = new Semaphore(0);
        pool.execute(() -> {
            try {
                hold.acquire();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        awaitActiveAtLeast(1);

        assertThat(registry.find("docgen.pdf.conversion.pool.active").gauge().value()).isEqualTo(1.0);
        assertThat(registry.find("docgen.pdf.conversion.pool.queue.remaining").gauge().value()).isZero();

        assertThatThrownBy(() -> service.convertWithResult(
                minimalDocxBytes(),
                PdfConversionOptions.stampingDisabled()
        ))
                .isInstanceOf(PdfConversionCapacityExceededException.class);
        assertThat(registry.find("docgen.pdf.conversion.pool.rejections").counter().count()).isEqualTo(1.0);

        hold.release();
        awaitActiveAtMost(0);

        byte[] pdf = service.convertWithResult(
                minimalDocxBytes(),
                PdfConversionOptions.stampingDisabled()
        ).pdfBytes();
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf)).startsWith("%PDF");
        assertThat(registry.find("docgen.pdf.conversion.pool.rejections").counter().count()).isEqualTo(1.0);
    }

    private void awaitActiveAtLeast(int expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (pool.getThreadPoolExecutor().getActiveCount() >= expected) {
                return;
            }
            sleepBriefly();
        }
        assertThat(pool.getThreadPoolExecutor().getActiveCount())
                .as("active workers")
                .isGreaterThanOrEqualTo(expected);
    }

    private void awaitActiveAtMost(int expected) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (pool.getThreadPoolExecutor().getActiveCount() <= expected) {
                return;
            }
            sleepBriefly();
        }
        assertThat(pool.getThreadPoolExecutor().getActiveCount())
                .as("active workers")
                .isLessThanOrEqualTo(expected);
    }

    private static void sleepBriefly() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadPoolTaskExecutor boundedPool(int poolSize, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("pdf-lo-chaos-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    private static Path resolveHangScript() throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase();
        String suffix = os.contains("win") ? ".cmd" : ".sh";
        return Path.of(LibreOfficePdfConversionPoolChaosIntegrationTest.class
                .getResource("/scripts/fake-libreoffice-hang" + suffix)
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
        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("IBL-D4 LO pool chaos");
            document.write(output);
            return output.toByteArray();
        }
    }
}
