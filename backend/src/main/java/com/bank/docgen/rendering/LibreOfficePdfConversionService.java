package com.bank.docgen.rendering;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import com.bank.docgen.template.service.TemplateValidationException;
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

    private final DocgenRenderingProperties renderingProperties;

    public LibreOfficePdfConversionService(DocgenRenderingProperties renderingProperties) {
        this.renderingProperties = renderingProperties;
    }

    @Override
    public byte[] convert(byte[] docxBytes) {
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
