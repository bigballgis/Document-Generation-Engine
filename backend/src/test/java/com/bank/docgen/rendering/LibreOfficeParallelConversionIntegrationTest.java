package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * F4-A1 / BDD-F4-A1-001: real {@code soffice} parallel conversion through a production-equivalent
 * bounded pool.
 *
 * <p>Default verify: optional skip when LibreOffice is unavailable. Mandatory CI lane:
 * {@code -Plibreoffice-ci} fails closed (IBL-D2 / F21).
 */
@Tag(LibreOfficeTestSupport.TAG)
class LibreOfficeParallelConversionIntegrationTest {

    private static final int CONCURRENCY = 4;

    private ThreadPoolTaskExecutor pool;

    @AfterEach
    void tearDown() {
        if (pool != null) {
            pool.shutdown();
        }
    }

    @Test
    void parallelConversionsThroughPool_allSucceed() throws Exception {
        LibreOfficeTestSupport.requireSoffice("LibreOfficeParallelConversionIntegrationTest / F4-A1");

        String soffice = LibreOfficeTestSupport.sofficeCommand();
        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setLibreOfficeCommand(soffice);
        properties.setConversionPoolSize(CONCURRENCY);
        properties.setConversionTimeoutSeconds(120);

        pool = productionEquivalentPool(CONCURRENCY);
        LibreOfficePdfConversionService service = new LibreOfficePdfConversionService(
                properties,
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                pool,
                new PdfConversionPostProcessor(properties, new DocxPdfConversionPreprocessor()),
                new PdfConversionPoolRejectionMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry())
        );

        long profileDirsBefore = countDocgenLoProfileDirs();
        byte[] docx = minimalDocxBytes();
        List<Future<byte[]>> futures = new ArrayList<>();
        ExecutorService callers = Executors.newFixedThreadPool(CONCURRENCY);
        try {
            for (int i = 0; i < CONCURRENCY; i++) {
                futures.add(callers.submit(() ->
                        service.convertWithResult(docx, PdfConversionOptions.stampingDisabled()).pdfBytes()));
            }
            for (Future<byte[]> future : futures) {
                byte[] pdf = future.get(properties.getConversionTimeoutSeconds() + 10L, TimeUnit.SECONDS);
                assertThat(pdf).isNotEmpty();
                assertThat(new String(pdf)).startsWith("%PDF");
                try (PDDocument document = Loader.loadPDF(pdf)) {
                    assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
                }
            }
        } finally {
            callers.shutdownNow();
        }

        assertThat(countDocgenLoProfileDirs())
                .as("profile directories must not leak after parallel conversions")
                .isEqualTo(profileDirsBefore);
    }

    private static ThreadPoolTaskExecutor productionEquivalentPool(int poolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("pdf-parallel-integration-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }

    private static byte[] minimalDocxBytes() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("F4 parallel conversion integration");
            document.write(output);
            return output.toByteArray();
        }
    }

    private static long countDocgenLoProfileDirs() throws IOException {
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> paths = Files.list(tempRoot)) {
            return paths.filter(path -> path.getFileName().toString().startsWith("docgen-lo-profile-")).count();
        }
    }
}
