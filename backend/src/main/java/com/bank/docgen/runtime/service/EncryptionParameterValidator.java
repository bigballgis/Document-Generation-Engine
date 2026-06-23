package com.bank.docgen.runtime.service;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.runtime.api.EncryptionOptionsView;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class EncryptionParameterValidator {

    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_PASSWORD_LENGTH = 128;

    public void validate(EncryptionOptionsView encryption, ApiPolicyEntity policy, String outputFormat) {
        if (encryption == null) {
            return;
        }
        if (encryption.enabled() == null) {
            rejectWhenPasswordFieldsPresent(encryption);
            return;
        }
        if (!encryption.enabled()) {
            rejectWhenPasswordFieldsPresent(encryption);
            return;
        }
        if (!isEncryptionAllowed(policy, outputFormat)) {
            throw new RuntimeEncryptionValidationException("api.error.encryption.encryptionNotAllowed");
        }
        if (isBlank(encryption.openPassword())) {
            throw new RuntimeEncryptionValidationException("api.error.encryption.openPasswordRequired");
        }
        validatePasswordLength(encryption.openPassword());
        if (encryption.ownerPassword() != null) {
            validatePasswordLength(encryption.ownerPassword());
            if (encryption.openPassword().equals(encryption.ownerPassword())) {
                throw new RuntimeEncryptionValidationException("api.error.encryption.passwordsMustDiffer");
            }
        }
        if (encryption.permissions() != null && !encryption.permissions().isEmpty()) {
            if (isBlank(encryption.ownerPassword())) {
                throw new RuntimeEncryptionValidationException("api.error.encryption.ownerPasswordRequired");
            }
            validatePermissions(encryption.permissions());
        }
    }

    private void rejectWhenPasswordFieldsPresent(EncryptionOptionsView encryption) {
        if (!isBlank(encryption.openPassword())
                || !isBlank(encryption.ownerPassword())
                || hasPermissions(encryption.permissions())) {
            throw new RuntimeEncryptionValidationException("api.error.encryption.encryptionParameterInvalid");
        }
    }

    private boolean isEncryptionAllowed(ApiPolicyEntity policy, String outputFormat) {
        if ("DOCX".equalsIgnoreCase(outputFormat)) {
            return policy.isDocxEncryptionEnabled();
        }
        if ("PDF".equalsIgnoreCase(outputFormat)) {
            return policy.isPdfEncryptionEnabled();
        }
        return false;
    }

    private void validatePasswordLength(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new RuntimeEncryptionValidationException("api.error.encryption.passwordLengthInvalid");
        }
    }

    private void validatePermissions(List<String> permissions) {
        for (String permission : permissions) {
            if (!isSupportedPermission(permission)) {
                throw new RuntimeEncryptionValidationException("api.error.encryption.permissionUnsupported");
            }
        }
    }

    private boolean isSupportedPermission(String permission) {
        return switch (permission) {
            case "ALLOW_PRINT", "ALLOW_COPY", "ALLOW_EDIT", "ALLOW_ANNOTATE", "ALLOW_FORM_FILL" -> true;
            default -> false;
        };
    }

    private boolean hasPermissions(List<String> permissions) {
        return permissions != null && !permissions.isEmpty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
