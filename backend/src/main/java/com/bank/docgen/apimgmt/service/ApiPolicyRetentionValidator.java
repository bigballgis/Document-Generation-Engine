package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.domain.ApiPolicyRetentionPresets;
import com.bank.docgen.template.service.TemplateValidationException;

final class ApiPolicyRetentionValidator {

    private ApiPolicyRetentionValidator() {
    }

    static void validate(boolean saveGeneratedDocuments, int invocationRecordRetentionDays, int documentRetentionDays) {
        if (!ApiPolicyRetentionPresets.INVOCATION_RECORD_RETENTION_DAYS.contains(invocationRecordRetentionDays)) {
            throw new TemplateValidationException("api.error.apimgmt.invocationRetentionPresetInvalid");
        }
        if (saveGeneratedDocuments) {
            if (!ApiPolicyRetentionPresets.DOCUMENT_RETENTION_DAYS.contains(documentRetentionDays)) {
                throw new TemplateValidationException("api.error.apimgmt.documentRetentionPresetInvalid");
            }
            if (documentRetentionDays > invocationRecordRetentionDays) {
                throw new TemplateValidationException("api.error.apimgmt.documentRetentionExceedsRecordRetention");
            }
        }
    }
}
