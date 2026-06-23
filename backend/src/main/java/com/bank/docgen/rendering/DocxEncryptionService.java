package com.bank.docgen.rendering;

import com.bank.docgen.runtime.api.EncryptionOptionsView;
import com.bank.docgen.runtime.service.RuntimeEncryptionFailedException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.stereotype.Service;

@Service
public class DocxEncryptionService {

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
        } catch (Exception ex) {
            throw new RuntimeEncryptionFailedException();
        }
    }
}
