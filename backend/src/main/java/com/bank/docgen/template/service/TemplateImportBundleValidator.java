package com.bank.docgen.template.service;

import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TemplateImportBundleValidator {

    private static final List<String> FORBIDDEN_SECRET_MARKERS = List.of(
            "credentialid",
            "clientsecret",
            "apikey",
            "passwordhash",
            "rawsecret"
    );

    private final ObjectMapper objectMapper;

    public TemplateImportBundleValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validate(TemplateExportBundleView bundle) {
        if (bundle == null) {
            throw new TemplateValidationException("api.error.template.importBundleInvalid");
        }
        if (!TemplateExportService.EXPORT_FORMAT.equals(bundle.format())) {
            throw new TemplateValidationException("api.error.template.importBundleUnsupportedFormat");
        }
        TemplateExportMetadataView metadata = bundle.metadata();
        if (metadata == null
                || isBlank(metadata.templateId())
                || isBlank(metadata.externalId())
                || isBlank(metadata.groupCode())
                || isBlank(metadata.name())) {
            throw new TemplateValidationException("api.error.template.importBundleInvalid");
        }
        if (bundle.variables() == null
                || bundle.bindings() == null
                || bundle.rules() == null
                || bundle.contentModuleReferences() == null) {
            throw new TemplateValidationException("api.error.template.importBundleInvalid");
        }
        assertNoSecrets(bundle);
    }

    private void assertNoSecrets(TemplateExportBundleView bundle) {
        try {
            String serialized = objectMapper.writeValueAsString(bundle).toLowerCase(Locale.ROOT);
            for (String marker : FORBIDDEN_SECRET_MARKERS) {
                if (serialized.contains(marker)) {
                    throw new TemplateValidationException("api.error.template.importBundleContainsSecrets");
                }
            }
        } catch (TemplateValidationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new TemplateValidationException("api.error.template.importBundleInvalid");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
