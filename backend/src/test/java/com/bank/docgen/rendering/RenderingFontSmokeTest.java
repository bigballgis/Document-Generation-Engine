package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.infrastructure.config.DocgenRenderingProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * LR-A2 / P23-T02 / BDD-DEMO-TYP-009–010: smoke test that mixed CJK + Calibri-styled Latin
 * DOCX converts to PDF without tofu when the Docker font baseline (Noto CJK + Carlito) is present.
 *
 * <p>Skipped when {@code soffice} is unavailable (local dev without LibreOffice). Runs in the
 * packaged backend image used by {@code docker-deploy.ps1}.
 */
class RenderingFontSmokeTest {

    private static final String CJK_SAMPLE = "中华人民共和国外资银行";
    private static final String LATIN_SAMPLE = "Meridian Bank plc";
    private static final Pattern TOFU_MARKERS = Pattern.compile("[\uFFFD\u25A1\u25A0]");

    @Test
    void mixedScriptDocxConvertsToPdfWithExtractableChinese() throws Exception {
        String soffice = System.getenv().getOrDefault("LIBREOFFICE_COMMAND", "soffice");
        if (!isSofficeAvailable(soffice)) {
            return;
        }

        DocgenRenderingProperties properties = new DocgenRenderingProperties();
        properties.setLibreOfficeCommand(soffice);
        properties.setConversionTimeoutSeconds(120);

        LibreOfficePdfConversionService conversionService = new LibreOfficePdfConversionService(
                properties,
                CircuitBreakerRegistry.ofDefaults(),
                RetryRegistry.ofDefaults(),
                conversionPool(),
                new PdfConversionPostProcessor(properties, new DocxPdfConversionPreprocessor())
        );

        byte[] docx = mixedScriptDocx();
        byte[] pdf = conversionService
                .convertWithResult(docx, PdfConversionOptions.stampingDisabled())
                .pdfBytes();

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf)).contains("%PDF");

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String extracted = new PDFTextStripper().getText(document).replaceAll("\\s+", "");
            assertThat(extracted)
                    .as("PDF text must contain the CJK sample without tofu markers")
                    .contains(normalizeForAssertion(CJK_SAMPLE))
                    .doesNotContainPattern(TOFU_MARKERS);
            assertThat(extracted)
                    .as("PDF text must retain the Calibri-styled Latin sample")
                    .containsIgnoringCase("Meridian")
                    .containsIgnoringCase("Bank");
        }
    }

    private static String normalizeForAssertion(String text) {
        return text.replaceAll("\\s+", "");
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

    private static byte[] mixedScriptDocx() throws IOException {
        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph latinParagraph = document.createParagraph();
            XWPFRun latinRun = latinParagraph.createRun();
            latinRun.setFontFamily("Calibri");
            latinRun.setText(LATIN_SAMPLE);

            XWPFParagraph cjkParagraph = document.createParagraph();
            XWPFRun cjkRun = cjkParagraph.createRun();
            cjkRun.setFontFamily("Calibri");
            cjkRun.setText(CJK_SAMPLE);

            document.write(output);
            return output.toByteArray();
        }
    }

    private static ThreadPoolTaskExecutor conversionPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(4);
        executor.setThreadNamePrefix("font-smoke-");
        executor.initialize();
        return executor;
    }
}
