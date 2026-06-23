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
import java.util.concurrent.TimeUnit;
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

    public LibreOfficePdfConversionService(
            DocgenRenderingProperties renderingProperties,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry
    ) {
        this.renderingProperties = renderingProperties;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE_INSTANCE);
        this.retry = retryRegistry.retry(RESILIENCE_INSTANCE);
    }

    @Override
    public byte[] convert(byte[] docxBytes) {
        return ResilienceSupport.execute(circuitBreaker, retry, () -> convertInternal(docxBytes));
    }

    private byte[] convertInternal(byte[] docxBytes) {
        try {
            Path tempDir = Files.createTempDirectory("docgen-pdf-");
            Path inputDocx = tempDir.resolve("input.docx");
            Files.write(inputDocx, docxBytes);
            ProcessBuilder processBuilder = new ProcessBuilder(
                    renderingProperties.getLibreOfficeCommand(),
                    "--headless",
                    "--convert-to",
                    "pdf",
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
                throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
            }
            Path outputPdf = tempDir.resolve("input.pdf");
            if (!Files.exists(outputPdf)) {
                throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
            }
            return Files.readAllBytes(outputPdf);
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
        }
    }
}
