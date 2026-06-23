package com.bank.docgen.rendering;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import java.io.ByteArrayInputStream;
import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

class DocxEncryptionServiceTest {

    @Test
    void encryptsDocxWithOpenPassword() throws Exception {
        byte[] plainDocx = buildSampleDocx("Encrypted content");
        DocxEncryptionService service = new DocxEncryptionService();
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                "SecretPass1234",
                null,
                null
        );

        byte[] encrypted = service.encrypt(plainDocx, encryption);

        assertNotEquals(plainDocx.length, encrypted.length);
        try (POIFSFileSystem fileSystem = new POIFSFileSystem(new ByteArrayInputStream(encrypted))) {
            EncryptionInfo info = new EncryptionInfo(fileSystem);
            Decryptor decryptor = info.getDecryptor();
            assertTrue(decryptor.verifyPassword("SecretPass1234"));
        }
    }

    private byte[] buildSampleDocx(String text) throws Exception {
        try (XWPFDocument document = new XWPFDocument();
                java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            XWPFRun run = paragraph.createRun();
            run.setText(text);
            document.write(output);
            return output.toByteArray();
        }
    }
}
