package com.bank.docgen.rendering;

import com.bank.docgen.runtime.api.EncryptionOptionsView;
import com.bank.docgen.runtime.service.RuntimeEncryptionFailedException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Service;

@Service
public class PdfEncryptionService {

    public byte[] encrypt(byte[] pdfBytes, EncryptionOptionsView encryption) {
        if (encryption == null || !Boolean.TRUE.equals(encryption.enabled())) {
            return pdfBytes;
        }
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            AccessPermission permissions = buildPermissions(encryption.permissions());
            String ownerPassword = encryption.ownerPassword() != null
                    ? encryption.ownerPassword()
                    : encryption.openPassword();
            StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    ownerPassword,
                    encryption.openPassword(),
                    permissions
            );
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                document.save(output);
                return output.toByteArray();
            }
        } catch (Exception ex) {
            throw new RuntimeEncryptionFailedException();
        }
    }

    private AccessPermission buildPermissions(List<String> permissionCodes) {
        AccessPermission permissions = new AccessPermission();
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            permissions.setCanPrint(true);
            permissions.setCanExtractContent(true);
            return permissions;
        }
        for (String permission : permissionCodes) {
            switch (permission) {
                case "ALLOW_PRINT" -> permissions.setCanPrint(true);
                case "ALLOW_COPY" -> permissions.setCanExtractContent(true);
                case "ALLOW_EDIT" -> permissions.setCanModify(true);
                case "ALLOW_ANNOTATE" -> permissions.setCanModifyAnnotations(true);
                case "ALLOW_FORM_FILL" -> permissions.setCanFillInForm(true);
                default -> {
                }
            }
        }
        return permissions;
    }
}
