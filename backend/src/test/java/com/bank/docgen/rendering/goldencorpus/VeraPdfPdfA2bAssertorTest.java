package com.bank.docgen.rendering.goldencorpus;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * IBL-B3 / F12 — veraPDF machine gate (not XMP-only {@link PdfAidXmpAssertor}).
 */
class VeraPdfPdfA2bAssertorTest {

    @BeforeEach
    void respectLocalSkipPolicy() {
        assumeTrue(
                VeraPdfPdfA2bAssertor.shouldValidateOrFailIfRequired(),
                "Local veraPDF skip (docgen.verapdf.required=false + docgen.verapdf.skip=true)"
        );
    }

    @Test
    void acceptsVeraPdfCorpusPdfA2bPassFixture() throws Exception {
        byte[] pdf = loadClasspathResource("/pdfa-fixtures/pdfa-2b-corpus-pass.pdf");

        assertThatCode(() -> VeraPdfPdfA2bAssertor.assertPdfA2b(pdf))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPlainPdfWithoutPdfA() throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(output);
            pdf = output.toByteArray();
        }

        assertThatThrownBy(() -> VeraPdfPdfA2bAssertor.assertPdfA2b(pdf))
                .isInstanceOf(GoldenCorpusException.class)
                .hasMessageContaining("veraPDF");
    }

    @Test
    void rejectsEmptyBytes() {
        assertThatThrownBy(() -> VeraPdfPdfA2bAssertor.assertPdfA2b(new byte[0]))
                .isInstanceOf(GoldenCorpusException.class)
                .hasMessageContaining("empty");
    }

    private static byte[] loadClasspathResource(String path) throws Exception {
        try (InputStream in = VeraPdfPdfA2bAssertorTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath fixture: " + path);
            }
            return in.readAllBytes();
        }
    }
}
