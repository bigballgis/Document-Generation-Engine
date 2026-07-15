package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.sharedkernel.api.EncryptionOptionsView;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CE-C06 structural encryption validation — hard failures remain; DOCX+permissions is not rejected.
 */
class EncryptionParameterValidatorTest {

    private static final String OPEN = "OpenPassword12";
    private static final String OWNER = "OwnerPassword12";

    private EncryptionParameterValidator validator;
    private ApiPolicyEntity policy;

    @BeforeEach
    void setUp() {
        validator = new EncryptionParameterValidator();
        policy = ApiPolicyEntity.createSkeleton(UUID.randomUUID(), "10000001");
        policy.replaceConfiguration(
                policy.getAllowedAdGroupsJson(),
                null,
                policy.getOutputFormatsJson(),
                policy.getOutputModesJson(),
                policy.isBatchEnabled(),
                policy.getMaxBatchSize(),
                true,
                true,
                "10000001"
        );
    }

    @Test
    void bddCeC06_002_docxWithPermissionsPassesStructuralValidation() {
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                OPEN,
                OWNER,
                List.of("ALLOW_PRINT", "ALLOW_COPY")
        );

        assertThatCode(() -> validator.validate(encryption, policy, "DOCX"))
                .doesNotThrowAnyException();
    }

    @Test
    void bddCeC06_006_missingOwnerPasswordStillRejectedForDocx() {
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                OPEN,
                null,
                List.of("ALLOW_PRINT")
        );

        assertThatThrownBy(() -> validator.validate(encryption, policy, "DOCX"))
                .isInstanceOf(RuntimeEncryptionValidationException.class)
                .extracting(ex -> ((RuntimeEncryptionValidationException) ex).messageKey())
                .isEqualTo("api.error.encryption.ownerPasswordRequired");
    }

    @Test
    void bddCeC06_007_enabledFalseWithPermissionsStillRejected() {
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                false,
                null,
                null,
                List.of("ALLOW_PRINT")
        );

        assertThatThrownBy(() -> validator.validate(encryption, policy, "DOCX"))
                .isInstanceOf(RuntimeEncryptionValidationException.class)
                .extracting(ex -> ((RuntimeEncryptionValidationException) ex).messageKey())
                .isEqualTo("api.error.encryption.encryptionParameterInvalid");
    }

    @Test
    void bddCeC06_008_illegalPermissionEnumStillRejected() {
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                OPEN,
                OWNER,
                List.of("ALLOW_PRINT", "ALLOW_HACK")
        );

        assertThatThrownBy(() -> validator.validate(encryption, policy, "PDF"))
                .isInstanceOf(RuntimeEncryptionValidationException.class)
                .extracting(ex -> ((RuntimeEncryptionValidationException) ex).messageKey())
                .isEqualTo("api.error.encryption.permissionUnsupported");
    }

    @Test
    void bddCeC06_004_docxWithoutPermissionsPasses() {
        EncryptionOptionsView encryption = new EncryptionOptionsView(
                true,
                OPEN,
                null,
                List.of()
        );

        assertThatCode(() -> validator.validate(encryption, policy, "DOCX"))
                .doesNotThrowAnyException();
    }
}
