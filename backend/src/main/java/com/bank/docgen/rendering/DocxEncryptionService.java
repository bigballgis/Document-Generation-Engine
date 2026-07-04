package com.bank.docgen.rendering;

import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DocxEncryptionService {

    private static final Logger LOG = LoggerFactory.getLogger(DocxEncryptionService.class);

    public byte[] encrypt(byte[] docxBytes, EncryptionOptionsView encryption) {
        if (encryption == null || !Boolean.TRUE.equals(encryption.enabled())) {
            return docxBytes;
        }
        try (POIFSFileSystem fileSystem = new POIFSFileSystem();
                OPCPackage document = OPCPackage.open(new ByteArrayInputStream(docxBytes))) {
            EncryptionInfo encryptionInfo = new EncryptionInfo(EncryptionMode.agile);
            Encryptor encryptor = encryptionInfo.getEncryptor();
            encryptor.confirmPassword(encryption.openPassword());
            try (OutputStream encryptedStream = encryptor.getDataStream(fileSystem)) {
                document.save(encryptedStream);
            }
            document.close();
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                fileSystem.writeFilesystem(output);
                return output.toByteArray();
            }
        } catch (IOException | GeneralSecurityException | OpenXML4JException ex) {
            LOG.warn("DOCX encryption failed: {}", ex.getMessage());
            throw new EncryptionFailedException();
        }
    }
}
