package com.bank.docgen.rendering;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
@ConditionalOnProperty(name = "docgen.rendering.conversion-mode", havingValue = "cli", matchIfMissing = true)
public class LibreOfficePdfConversionService implements PdfConversionService {

    private static final String RESILIENCE_INSTANCE = "pdfConversion";

    private final DocgenRenderingProperties renderingProperties;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Executor pdfConversionExecutor;
    private final PdfConversionPostProcessor pdfConversionPostProcessor;
    private final PdfConversionPoolRejectionMetrics poolRejectionMetrics;

    public LibreOfficePdfConversionService(
            DocgenRenderingProperties renderingProperties,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            @Qualifier("pdfConversionExecutor") Executor pdfConversionExecutor,
            PdfConversionPostProcessor pdfConversionPostProcessor,
            PdfConversionPoolRejectionMetrics poolRejectionMetrics
    ) {
        this.renderingProperties = renderingProperties;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE_INSTANCE);
        this.retry = retryRegistry.retry(RESILIENCE_INSTANCE);
        this.pdfConversionExecutor = pdfConversionExecutor;
        this.pdfConversionPostProcessor = pdfConversionPostProcessor;
        this.poolRejectionMetrics = poolRejectionMetrics;
    }

    @Override
    public DocumentArtifactPipeline.PdfConversionResult convertWithResult(
            byte[] docxBytes,
            PdfConversionOptions options
    ) {
        PdfConversionOptions resolvedOptions = options == null
                ? PdfConversionOptions.stampingDisabled()
                : options;
        return ResilientPdfConversionSupport.convertWithResilience(
                circuitBreaker,
                retry,
                pdfConversionExecutor,
                renderingProperties.getConversionTimeoutSeconds(),
                () -> convertInternal(docxBytes, resolvedOptions),
                poolRejectionMetrics::record
        );
    }

    private DocumentArtifactPipeline.PdfConversionResult convertInternal(
            byte[] docxBytes,
            PdfConversionOptions options
    ) {
        Path tempDir = null;
        Path profileDir = null;
        try {
            tempDir = Files.createTempDirectory("docgen-pdf-");
            // LR-A1: per-invocation profile isolation — concurrent pooled conversions must not share
            // one LibreOffice user profile (CD-PIT-11: the industry's top intermittent headless
            // conversion failure class). Each invocation gets its own -env:UserInstallation profile.
            profileDir = Files.createTempDirectory("docgen-lo-profile-");
            Path inputDocx = tempDir.resolve("input.docx");
            byte[] pdfSourceDocx = pdfConversionPostProcessor.prepareDocxForConversion(docxBytes, options);
            Files.write(inputDocx, pdfSourceDocx);
            ProcessBuilder processBuilder = new ProcessBuilder(buildCliArguments(
                    renderingProperties.getLibreOfficeCommand(),
                    profileDir,
                    inputDocx,
                    tempDir,
                    options
            ));
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            boolean finished = process.waitFor(
                    renderingProperties.getConversionTimeoutSeconds(),
                    TimeUnit.SECONDS
            );
            if (!finished || process.exitValue() != 0) {
                throw new RenderingOperationException("api.error.generation.pdfConversionFailed");
            }
            Path outputPdf = tempDir.resolve("input.pdf");
            if (!Files.exists(outputPdf)) {
                throw new RenderingOperationException("api.error.generation.pdfConversionFailed");
            }
            byte[] converted = Files.readAllBytes(outputPdf);
            PdfPageStampResult stampResult = pdfConversionPostProcessor.finishPdf(converted, options);
            return DocumentArtifactPipeline.PdfConversionResult.of(stampResult.pdfBytes(), stampResult);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RenderingOperationException("api.error.generation.pdfConversionFailed");
        } catch (IOException ex) {
            throw new RenderingOperationException("api.error.generation.pdfConversionFailed");
        } finally {
            // Recursive delete: on Windows, non-empty temp dirs (or briefly locked children)
            // make Files.deleteIfExists(tempDir) a silent no-op and flake cleanup asserts.
            if (tempDir != null) {
                deleteProfileTree(tempDir);
            }
            if (profileDir != null) {
                deleteProfileTree(profileDir);
            }
        }
    }

    /**
     * Build LibreOffice CLI argv (CE-O01 archival filter selection is asserted via this helper).
     */
    static java.util.List<String> buildCliArguments(
            String libreOfficeCommand,
            Path profileDir,
            Path inputDocx,
            Path outDir,
            PdfConversionOptions options
    ) {
        PdfConversionOptions resolved = options == null
                ? PdfConversionOptions.stampingDisabled()
                : options;
        return java.util.List.of(
                libreOfficeCommand,
                "--headless",
                "-env:UserInstallation=" + profileUrl(profileDir),
                "--norestore",
                "--nolockcheck",
                "--nodefault",
                "--nologo",
                "--convert-to",
                LibreOfficePdfExportFilters.convertToArgument(resolved.pdfArchivalProfile()),
                "--outdir",
                outDir.toString(),
                inputDocx.toString()
        );
    }

    /**
     * Build the {@code file://} URL LibreOffice expects for {@code -env:UserInstallation}. The path
     * must be absolute, URL-encoded, and use forward slashes — also on Windows where {@code Path}
     * uses backslashes.
     */
    static String profileUrl(Path profileDir) {
        String absolute = profileDir.toAbsolutePath().toString().replace('\\', '/');
        StringBuilder encoded = new StringBuilder("file:///");
        for (int i = 0; i < absolute.length(); i++) {
            char c = absolute.charAt(i);
            if (c == ':' || c == ' ' || c == '[' || c == ']' || c == '{' || c == '}') {
                encoded.append(String.format("%%%02X", (int) c));
            } else {
                encoded.append(c);
            }
        }
        return encoded.toString();
    }

    /**
     * LibreOffice populates the profile dir with a tree (user/, registrymodifications.xcu, lock
     * files). Best-effort recursive delete — mirrors temp cleanup tolerance of failures.
     */
    private static void deleteProfileTree(Path profileDir) {
        try (Stream<Path> walk = Files.walk(profileDir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
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
