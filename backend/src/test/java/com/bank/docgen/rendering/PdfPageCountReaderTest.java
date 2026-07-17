package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

class PdfPageCountReaderTest {

    private final PdfPageCountReader reader = new PdfPageCountReader();

    @Test
    void countsPagesInValidPdf() throws IOException {
        byte[] pdf = pdfWithPages(3);

        assertThat(reader.countPages(pdf)).isEqualTo(3);
    }

    @Test
    void returnsNullForEmptyOrCorruptInput() {
        assertThat(reader.countPages(null)).isNull();
        assertThat(reader.countPages(new byte[0])).isNull();
        assertThat(reader.countPages(new byte[] {1, 2, 3})).isNull();
    }

    private static byte[] pdfWithPages(int pages) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages; i++) {
                document.addPage(new PDPage());
            }
            document.save(out);
            return out.toByteArray();
        }
    }
}
