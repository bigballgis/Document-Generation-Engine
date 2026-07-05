package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.rendering.DocumentArtifactPipeline;
import com.bank.docgen.rendering.PdfConversionOptions;
import com.bank.docgen.rendering.PdfConversionService;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * LR-D6: load smoke baseline. Submits ≥20 concurrent sync generations (DOCX→PDF) through the
 * conversion pool and asserts they all succeed within the conversion timeout.
 *
 * <p>This is a smoke test, not a perf benchmark — it verifies the LR-A1 profile isolation
 * holds under concurrency and the pool does not deadlock under realistic load. It runs only
 * when a real {@code soffice} is on the PATH (CI Docker image); otherwise it is skipped.
 */
class LoadSmokeBaselineTest {

    @Test
    void twentyConcurrentConversionsAllSucceed() throws Exception {
        String soffice = System.getenv().getOrDefault("LIBREOFFICE_COMMAND", "soffice");
        if (!isSofficeAvailable(soffice)) {
            // Local dev without LibreOffice installed — skip the load smoke.
            return;
        }

        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setLibreOfficeCommand(soffice);
        properties.setConversionPoolSize(2);
        properties.setConversionTimeoutSeconds(120);

        PdfConversionService conversionService = new com.bank.docgen.rendering.LibreOfficePdfConversionService(
                properties,
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                poolOfSize(2),
                new com.bank.docgen.rendering.PdfConversionPostProcessor(
                        properties,
                        new com.bank.docgen.rendering.DocxPdfConversionPreprocessor()
                )
        );

        byte[] docx = minimalDocx();
        int concurrency = 20;
        ExecutorService caller = Executors.newFixedThreadPool(concurrency);
        List<Future<byte[]>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < concurrency; i++) {
                futures.add(caller.submit(() ->
                        conversionService.convertWithResult(docx, PdfConversionOptions.stampingDisabled()).pdfBytes()));
            }
            for (Future<byte[]> future : futures) {
                byte[] pdf = future.get(180, TimeUnit.SECONDS);
                assertThat(pdf).isNotEmpty();
                assertThat(new String(pdf)).contains("%PDF");
            }
        } finally {
            caller.shutdownNow();
        }
    }

    private static boolean isSofficeAvailable(String command) {
        try {
            Process process = new ProcessBuilder(command, "--version").redirectErrorStream(true).start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private static byte[] minimalDocx() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Load smoke baseline body");
            document.write(output);
            return output.toByteArray();
        }
    }

    private static ThreadPoolTaskExecutor poolOfSize(int size) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(size);
        executor.setMaxPoolSize(size);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("load-smoke-");
        executor.initialize();
        return executor;
    }
}
