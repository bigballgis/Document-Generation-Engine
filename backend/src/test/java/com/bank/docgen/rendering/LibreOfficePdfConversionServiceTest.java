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
import java.util.Comparator;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LibreOfficePdfConversionServiceTest {

    private DocgenRenderingProperties properties;
    private Path fakeLibreOfficeScript;

    @BeforeEach
    void setUp() throws URISyntaxException {
        properties = new DocgenRenderingProperties();
        fakeLibreOfficeScript = Path.of(
                LibreOfficePdfConversionServiceTest.class
                        .getResource("/scripts/fake-libreoffice.cmd")
                        .toURI()
        );
    }

    @AfterEach
    void tearDown() throws IOException {
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

        byte[] pdf = service.convert(new byte[]{1, 2, 3});

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf)).contains("%PDF");
        assertThat(countDocgenPdfTempDirs()).isEqualTo(tempDirsBefore);
    }

    @Test
    void removesTempDirectoryAfterFailedConversion() throws URISyntaxException, IOException {
        Path failScript = Path.of(
                LibreOfficePdfConversionServiceTest.class
                        .getResource("/scripts/fake-libreoffice-fail.cmd")
                        .toURI()
        );
        properties.setLibreOfficeCommand(failScript.toString());
        properties.setConversionTimeoutSeconds(30);
        LibreOfficePdfConversionService service = service();
        long tempDirsBefore = countDocgenPdfTempDirs();

        assertThatThrownBy(() -> service.convert(new byte[]{1}))
                .isInstanceOf(TemplateValidationException.class);
        assertThat(countDocgenPdfTempDirs()).isEqualTo(tempDirsBefore);
    }

    @Test
    void rejectsNonZeroExitCode() throws URISyntaxException {
        Path failScript = Path.of(
                LibreOfficePdfConversionServiceTest.class
                        .getResource("/scripts/fake-libreoffice-fail.cmd")
                        .toURI()
        );
        properties.setLibreOfficeCommand(failScript.toString());
        properties.setConversionTimeoutSeconds(30);
        LibreOfficePdfConversionService service = service();

        assertThatThrownBy(() -> service.convert(new byte[]{1}))
                .isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void rejectsTimedOutConversion() {
        properties.setLibreOfficeCommand("ping");
        properties.setConversionTimeoutSeconds(1);
        LibreOfficePdfConversionService service = service();

        assertThatThrownBy(() -> service.convert(new byte[]{1}))
                .isInstanceOf(TemplateValidationException.class);
    }

    private LibreOfficePdfConversionService service() {
        return new LibreOfficePdfConversionService(
                properties,
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults()
        );
    }

    private long countDocgenPdfTempDirs() throws IOException {
        Path tempRoot = Path.of(System.getProperty("java.io.tmpdir"));
        try (Stream<Path> paths = Files.list(tempRoot)) {
            return paths.filter(path -> path.getFileName().toString().startsWith("docgen-pdf-")).count();
        }
    }
}
