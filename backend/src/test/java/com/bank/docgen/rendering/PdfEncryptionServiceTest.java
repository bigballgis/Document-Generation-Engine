package com.bank.docgen.rendering;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.Test;

class PdfEncryptionServiceTest {

    @Test
    void encryptsPdfWithOpenPassword() throws Exception {
        byte[] plainPdf = buildSamplePdf();
        PdfEncryptionService service = new PdfEncryptionService();
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                "SecretPass1234",
                null,
                null
        );

        byte[] encrypted = service.encrypt(plainPdf, encryption);

        assertNotEquals(plainPdf.length, encrypted.length);
        assertThrows(Exception.class, () -> Loader.loadPDF(encrypted));
        try (PDDocument document = Loader.loadPDF(encrypted, "SecretPass1234")) {
            org.junit.jupiter.api.Assertions.assertEquals(1, document.getNumberOfPages());
        }
    }

    @Test
    void appliesPermissionFlagsWhenOwnerPasswordProvided() throws Exception {
        byte[] plainPdf = buildSamplePdf();
        PdfEncryptionService service = new PdfEncryptionService();
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                "SecretPass1234",
                "OwnerPass12345",
                List.of("ALLOW_PRINT")
        );

        byte[] encrypted = service.encrypt(plainPdf, encryption);

        try (PDDocument document = Loader.loadPDF(encrypted, "SecretPass1234")) {
            org.junit.jupiter.api.Assertions.assertTrue(document.getCurrentAccessPermission().canPrint());
        }
    }

    private byte[] buildSamplePdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage(PDRectangle.A4));
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                document.save(output);
                return output.toByteArray();
            }
        }
    }
}
