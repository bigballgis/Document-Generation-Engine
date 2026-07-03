package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.template.service.TemplateValidationException;
import org.junit.jupiter.api.Test;

class ApiPolicyRetentionValidatorTest {

    @Test
    void validate_acceptsSupportedPresetsWhenSaveEnabled() {
        assertThatCode(() -> ApiPolicyRetentionValidator.validate(true, 90, 30))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsUnsupportedInvocationRetentionPreset() {
        assertThatThrownBy(() -> ApiPolicyRetentionValidator.validate(true, 45, 30))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessageContaining("invocationRetentionPresetInvalid");
    }

    @Test
    void validate_rejectsDocumentRetentionExceedingRecordRetention() {
        assertThatThrownBy(() -> ApiPolicyRetentionValidator.validate(true, 30, 90))
                .isInstanceOf(TemplateValidationException.class)
                .hasMessageContaining("documentRetentionExceedsRecordRetention");
    }

    @Test
    void validate_allowsSaveDisabledWithoutDocumentPresetCheck() {
        assertThatCode(() -> ApiPolicyRetentionValidator.validate(false, 365, 999))
                .doesNotThrowAnyException();
    }
}
