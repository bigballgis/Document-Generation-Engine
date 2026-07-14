package com.bank.docgen.rendering;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

/**
 * BDD-CE-G02-DOCX-001 / X-004 — SPECIMEN in header and footer parts.
 */
class DocxSpecimenWatermarkStamperTest {

    @Test
    void stampsSpecimenIntoHeaderAndFooterWhenMissing() throws Exception {
        byte[] source = buildDocxWithoutHeadersOrFooters("Formal body text");

        byte[] stamped = DocxSpecimenWatermarkStamper.apply(source);

        assertThat(anyZipPartContains(stamped, "word/header", "SPECIMEN")).isTrue();
        assertThat(anyZipPartContains(stamped, "word/footer", "SPECIMEN")).isTrue();
        assertThat(readZipPart(stamped, "word/document.xml")).contains("Formal body text");
    }

    @Test
    void appendsSpecimenWithoutRemovingExistingHeaderFooterContent() throws Exception {
        byte[] source = buildDocxWithBusinessHeaderFooter("Bank letterhead", "Confidential footer");

        byte[] stamped = DocxSpecimenWatermarkStamper.apply(source);

        assertThat(anyZipPartContains(stamped, "word/header", "Bank letterhead")).isTrue();
        assertThat(anyZipPartContains(stamped, "word/header", "SPECIMEN")).isTrue();
        assertThat(anyZipPartContains(stamped, "word/footer", "Confidential footer")).isTrue();
        assertThat(anyZipPartContains(stamped, "word/footer", "SPECIMEN")).isTrue();
    }

    @Test
    void failsClosedOnCorruptDocx() {
        assertThatThrownBy(() -> DocxSpecimenWatermarkStamper.apply(new byte[] {1, 2, 3}))
                .isInstanceOf(DocxAssemblyException.class);
    }

    private static byte[] buildDocxWithoutHeadersOrFooters(String body) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(body);
            document.write(output);
            return output.toByteArray();
        }
    }

    private static byte[] buildDocxWithBusinessHeaderFooter(String headerText, String footerText) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("Body");
            document.createHeader(HeaderFooterType.DEFAULT).createParagraph().createRun().setText(headerText);
            document.createFooter(HeaderFooterType.DEFAULT).createParagraph().createRun().setText(footerText);
            document.write(output);
            return output.toByteArray();
        }
    }

    private static boolean anyZipPartContains(byte[] docxBytes, String namePrefix, String substring) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.startsWith(namePrefix) && name.endsWith(".xml")) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    if (xml.contains(substring)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String readZipPart(byte[] docxBytes, String partName) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(docxBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (partName.equals(entry.getName())) {
                    return new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new AssertionError("Missing zip part: " + partName);
    }
}
