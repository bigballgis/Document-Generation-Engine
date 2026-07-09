package com.bank.docgen.rendering;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.infrastructure.resilience.ResilienceSupport;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Optional LibreOffice round-trip for assembled DOCX. Disabled by default because LO rewrites a minimal
 * package (drops {@code styles.xml}, theme, fonts) that Microsoft Word renders as a blank document.
 */
@Service
@Profile("!test")
@ConditionalOnProperty(name = "docgen.rendering.docx-normalization-enabled", havingValue = "true")
public class LibreOfficeDocxNormalizationService implements DocxNormalizationService {

    private static final String RESILIENCE_INSTANCE = "docxNormalization";

    private final DocgenRenderingProperties renderingProperties;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Executor pdfConversionExecutor;

    public LibreOfficeDocxNormalizationService(
            DocgenRenderingProperties renderingProperties,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            @Qualifier("pdfConversionExecutor") Executor pdfConversionExecutor
    ) {
        this.renderingProperties = renderingProperties;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE_INSTANCE);
        this.retry = retryRegistry.retry(RESILIENCE_INSTANCE);
        this.pdfConversionExecutor = pdfConversionExecutor;
    }

    @Override
    public byte[] normalize(byte[] docxBytes) {
        return ResilienceSupport.execute(circuitBreaker, retry, () -> PdfConversionOffloadSupport.executeOffloaded(
                pdfConversionExecutor,
                renderingProperties.getConversionTimeoutSeconds(),
                () -> normalizeInternal(docxBytes)
        ));
    }

    private byte[] normalizeInternal(byte[] docxBytes) {
        Path tempDir = null;
        Path profileDir = null;
        try {
            tempDir = Files.createTempDirectory("docgen-docx-normalize-");
            // LR-A1: per-invocation profile isolation (CD-PIT-11) — same rationale as the PDF
            // conversion service; normalization also drives a soffice process.
            profileDir = Files.createTempDirectory("docgen-lo-norm-profile-");
            Path inputDocx = tempDir.resolve("assembled-in.docx");
            Files.write(inputDocx, docxBytes);
            ProcessBuilder processBuilder = new ProcessBuilder(
                    renderingProperties.getLibreOfficeCommand(),
                    "--headless",
                    "-env:UserInstallation=" + LibreOfficePdfConversionService.profileUrl(profileDir),
                    "--norestore",
                    "--nolockcheck",
                    "--nodefault",
                    "--nologo",
                    "--convert-to",
                    "docx",
                    "--outdir",
                    tempDir.toString(),
                    inputDocx.toString()
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            boolean finished = process.waitFor(
                    renderingProperties.getConversionTimeoutSeconds(),
                    TimeUnit.SECONDS
            );
            if (!finished || process.exitValue() != 0) {
                throw new RenderingOperationException("api.error.generation.docxNormalizationFailed");
            }
            Path outputDocx = tempDir.resolve("assembled-in.docx");
            if (!Files.exists(outputDocx) || Files.size(outputDocx) == 0) {
                throw new RenderingOperationException("api.error.generation.docxNormalizationFailed");
            }
            return Files.readAllBytes(outputDocx);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RenderingOperationException("api.error.generation.docxNormalizationFailed");
        } catch (IOException ex) {
            throw new RenderingOperationException("api.error.generation.docxNormalizationFailed");
        } finally {
            if (tempDir != null) {
                try {
                    Files.deleteIfExists(tempDir.resolve("assembled-in.docx"));
                    Files.deleteIfExists(tempDir);
                } catch (IOException ignored) {
                    // Best-effort temp cleanup.
                }
            }
            if (profileDir != null) {
                deleteProfileTree(profileDir);
            }
        }
    }

    private static void deleteProfileTree(Path profileDir) {
        try (java.util.stream.Stream<Path> walk = Files.walk(profileDir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Best-effort profile cleanup.
                }
            });
        } catch (IOException ignored) {
            // Best-effort profile cleanup.
        }
    }
}
