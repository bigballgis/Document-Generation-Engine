package com.bank.docgen.rendering;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.infrastructure.resilience.ResilienceSupport;
import com.bank.docgen.template.service.TemplateValidationException;
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

@Service
@Profile("!test")
@ConditionalOnProperty(name = "docgen.rendering.conversion-mode", havingValue = "docker-exec")
public class DockerExecPdfConversionService implements PdfConversionService {

    private static final String RESILIENCE_INSTANCE = "pdfConversion";

    private final DocgenRenderingProperties renderingProperties;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Executor pdfConversionExecutor;
    private final PdfConversionPostProcessor pdfConversionPostProcessor;

    public DockerExecPdfConversionService(
            DocgenRenderingProperties renderingProperties,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            @Qualifier("pdfConversionExecutor") Executor pdfConversionExecutor,
            PdfConversionPostProcessor pdfConversionPostProcessor
    ) {
        this.renderingProperties = renderingProperties;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE_INSTANCE);
        this.retry = retryRegistry.retry(RESILIENCE_INSTANCE);
        this.pdfConversionExecutor = pdfConversionExecutor;
        this.pdfConversionPostProcessor = pdfConversionPostProcessor;
    }

    @Override
    public DocumentArtifactPipeline.PdfConversionResult convertWithResult(
            byte[] docxBytes,
            PdfConversionOptions options
    ) {
        PdfConversionOptions resolvedOptions = options == null
                ? PdfConversionOptions.stampingDisabled()
                : options;
        return ResilienceSupport.execute(circuitBreaker, retry, () -> PdfConversionOffloadSupport.executeOffloaded(
                pdfConversionExecutor,
                renderingProperties.getConversionTimeoutSeconds(),
                () -> convertInternal(docxBytes, resolvedOptions)
        ));
    }

    private DocumentArtifactPipeline.PdfConversionResult convertInternal(
            byte[] docxBytes,
            PdfConversionOptions options
    ) {
        Path hostDir = null;
        try {
            hostDir = Files.createTempDirectory("docgen-docker-pdf-");
            Path inputDocx = hostDir.resolve("input.docx");
            byte[] pdfSourceDocx = pdfConversionPostProcessor.prepareDocxForConversion(docxBytes, options);
            Files.write(inputDocx, pdfSourceDocx);
            String container = renderingProperties.getDockerContainerName();
            String containerInput = "/tmp/docgen-input.docx";
            // LR-A1: per-invocation profile isolation inside the LibreOffice sidecar container
            // (CD-PIT-11). A unique container profile path prevents concurrent conversions from
            // sharing one soffice user profile and deadlocking on lock files. Derived from the
            // unique host temp dir name so it cannot collide across pooled invocations.
            String containerProfile = "/tmp/docgen-lo-profile-" + hostDir.getFileName().toString();

            runCommand(renderingProperties.getDockerCliCommand(), "cp", inputDocx.toString(), container + ":" + containerInput);
            runCommand(
                    renderingProperties.getDockerCliCommand(), "exec", container,
                    renderingProperties.getLibreOfficeCommand(),
                    "--headless",
                    "-env:UserInstallation=file://" + containerProfile,
                    "--norestore",
                    "--nolockcheck",
                    "--nodefault",
                    "--nologo",
                    "--convert-to", "pdf",
                    "--outdir", "/tmp",
                    containerInput
            );
            Path outputPdf = hostDir.resolve("input.pdf");
            runCommand(renderingProperties.getDockerCliCommand(), "cp", container + ":/tmp/input.pdf", outputPdf.toString());
            if (!Files.exists(outputPdf)) {
                throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
            }
            byte[] converted = Files.readAllBytes(outputPdf);
            PdfPageStampResult stampResult = pdfConversionPostProcessor.finishPdf(converted, options);
            return DocumentArtifactPipeline.PdfConversionResult.of(stampResult.pdfBytes(), stampResult);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
        } catch (IOException ex) {
            throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
        } finally {
            if (hostDir != null) {
                try {
                    Files.deleteIfExists(hostDir.resolve("input.docx"));
                    Files.deleteIfExists(hostDir.resolve("input.pdf"));
                    Files.deleteIfExists(hostDir);
                } catch (IOException ignored) {
                    // Best-effort temp cleanup.
                }
            }
        }
    }

    private void runCommand(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        boolean finished = process.waitFor(renderingProperties.getConversionTimeoutSeconds(), TimeUnit.SECONDS);
        if (!finished || process.exitValue() != 0) {
            throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
        }
    }
}
