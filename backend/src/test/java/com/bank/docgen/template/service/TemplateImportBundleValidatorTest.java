package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.api.TemplateExportBundleView;
import com.bank.docgen.template.api.TemplateExportMetadataView;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.domain.VariableType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TemplateImportBundleValidatorTest {

    private TemplateImportBundleValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TemplateImportBundleValidator(new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void validate_acceptsExportBundleShape() {
        assertThatCode(() -> validator.validate(validBundle(List.of()))).doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsUnsupportedFormat() {
        TemplateExportBundleView bundle = new TemplateExportBundleView(
                "legacy-format",
                validBundle(List.of()).metadata(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null
        );

        assertThatThrownBy(() -> validator.validate(bundle))
                .isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void validate_rejectsMissingMetadata() {
        TemplateExportBundleView bundle = new TemplateExportBundleView(
                TemplateExportService.EXPORT_FORMAT,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null
        );

        assertThatThrownBy(() -> validator.validate(bundle))
                .isInstanceOf(TemplateValidationException.class);
    }

    @Test
    void validate_rejectsSecretFields() {
        ApiPolicyView policyWithSecret = new ApiPolicyView(
                UUID.randomUUID().toString(),
                1,
                List.of("RETAIL_API"),
                "1.0.0",
                List.of("DOCX"),
                List.of("SYNC_STREAM"),
                false,
                10,
                100,
                10000,
                false,
                false,
                true,
                90,
                30,
                Instant.now()
        );
        TemplateExportBundleView bundle = validBundle(List.of(
                new CompositionRuleView("rule-1", "true", "HEADER", "", "")
        ));
        TemplateExportBundleView tampered = new TemplateExportBundleView(
                bundle.format(),
                bundle.metadata(),
                bundle.variables(),
                List.of(new AnchorBindingView(
                        UUID.randomUUID().toString(),
                        "HEADER",
                        "TEXT",
                        "{\"credentialId\":\"abc\"}",
                        BindingValidationStatus.VALID
                )),
                bundle.rules(),
                bundle.contentModuleReferences(),
                policyWithSecret
        );

        assertThatThrownBy(() -> validator.validate(tampered))
                .isInstanceOf(TemplateValidationException.class);
    }

    private TemplateExportBundleView validBundle(List<CompositionRuleView> rules) {
        UUID templateId = UUID.randomUUID();
        return new TemplateExportBundleView(
                TemplateExportService.EXPORT_FORMAT,
                new TemplateExportMetadataView(
                        templateId.toString(),
                        "TPL-IMPORT-LETTER",
                        "RETAIL",
                        "Import Letter",
                        "Import test template",
                        UUID.randomUUID().toString(),
                        TemplateLifecycleStatus.PUBLISHED,
                        "1.0.0",
                        UUID.randomUUID().toString(),
                        1,
                        Instant.now()
                ),
                List.of(new VariableSchemaView(
                        UUID.randomUUID().toString(),
                        "customerName",
                        VariableType.TEXT,
                        true,
                        "Customer",
                        null,
                        "Customer name",
                        null
                )),
                List.of(new AnchorBindingView(
                        UUID.randomUUID().toString(),
                        "HEADER",
                        "TEXT",
                        "{\"nodes\":[]}",
                        BindingValidationStatus.VALID
                )),
                rules,
                List.of(),
                null
        );
    }
}
