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
@ConditionalOnProperty(name = "docgen.rendering.conversion-mode", havingValue = "docker-exec")
public class DockerExecPdfConversionService implements PdfConversionService {

    private final DocgenRenderingProperties renderingProperties;

    public DockerExecPdfConversionService(DocgenRenderingProperties renderingProperties) {
        this.renderingProperties = renderingProperties;
    }

    @Override
    public byte[] convert(byte[] docxBytes) {
        Path hostDir = null;
        try {
            hostDir = Files.createTempDirectory("docgen-docker-pdf-");
            Path inputDocx = hostDir.resolve("input.docx");
            Files.write(inputDocx, docxBytes);
            String container = renderingProperties.getDockerContainerName();
            String containerInput = "/tmp/docgen-input.docx";

            runCommand("docker", "cp", inputDocx.toString(), container + ":" + containerInput);
            runCommand(
                    "docker", "exec", container,
                    renderingProperties.getLibreOfficeCommand(),
                    "--headless",
                    "--convert-to", "pdf",
                    "--outdir", "/tmp",
                    containerInput
            );
            Path outputPdf = hostDir.resolve("input.pdf");
            runCommand("docker", "cp", container + ":/tmp/input.pdf", outputPdf.toString());
            if (!Files.exists(outputPdf)) {
                throw new TemplateValidationException("api.error.generation.pdfConversionFailed");
            }
            return Files.readAllBytes(outputPdf);
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
