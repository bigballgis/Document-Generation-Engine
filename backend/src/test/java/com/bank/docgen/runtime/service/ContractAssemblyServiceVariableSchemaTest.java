package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.CallableVersionView;
import com.bank.docgen.runtime.api.ContractResultView;
import com.bank.docgen.runtime.api.ContractVariableSchemaView;
import com.bank.docgen.runtime.domain.ContractViewAudience;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.domain.VariablePiiCategory;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-IBL-A4-001…005 — /contract per-field VariableSchema projection.
 */
@ExtendWith(MockitoExtension.class)
class ContractAssemblyServiceVariableSchemaTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private MessageResolver messageResolver;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private VariableSchemaRepository variableSchemaRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ContractAssemblyService service;
    private TemplateEntity template;
    private ApiPolicyEntity policy;

    @BeforeEach
    void setUp() {
        service = new ContractAssemblyService(
                messageResolver,
                objectMapper,
                templateVersionRepository,
                variableSchemaRepository
        );
        template = new TemplateEntity(
                TEMPLATE_ID,
                "TPL-LETTER",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        template.setReleaseVersion("1.0.0");
        policy = new ApiPolicyEntity(UUID.randomUUID(), TEMPLATE_ID, "[\"RETAIL_API\"]", "10000001");
        policy.update(
                "[\"RETAIL_API\"]",
                "1.0.0",
                "[\"DOCX\"]",
                "[\"SYNC_STREAM\"]",
                false,
                10,
                false,
                false,
                "10000001"
        );
    }

    @Test
    void assembleProjectsPerFieldVariableSchemasSortedByKey() {
        TemplateVersionEntity version = publishedVersion("1.0.0");
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(TEMPLATE_ID))
                .thenReturn(List.of(version));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(VERSION_ID))
                .thenReturn(List.of(
                        schema("zebra", VariableType.TEXT, false, null, null, VariablePiiCategory.NONE),
                        schema("customerName", VariableType.TEXT, true, null, null, VariablePiiCategory.NONE),
                        schema(
                                "letterType",
                                VariableType.ENUM,
                                true,
                                "[\"OFFER\",\"REMINDER\"]",
                                null,
                                VariablePiiCategory.NONE
                        ),
                        schema(
                                "idNumber",
                                VariableType.TEXT,
                                true,
                                null,
                                null,
                                VariablePiiCategory.GOVERNMENT_ID
                        ),
                        schema(
                                "amountInWords",
                                VariableType.COMPUTED,
                                false,
                                null,
                                "SPELL_AMOUNT(principalAmount)",
                                VariablePiiCategory.NONE
                        ),
                        schema(
                                "middle",
                                VariableType.TEXT,
                                false,
                                null,
                                "FORMAT_AMOUNT(x)",
                                VariablePiiCategory.NONE
                        )
                ));

        ContractResultView contract = service.assemble(
                template,
                policy,
                "dev",
                null,
                ContractViewAudience.CALLER
        );

        assertThat(contract.schemas()).contains(
                "GenerateRequest",
                "BatchGenerateRequest",
                "OutputOptions",
                "EncryptionOptions"
        );
        CallableVersionView callable = contract.callableVersions().stream()
                .filter(view -> "1.0.0".equals(view.releaseVersion()))
                .findFirst()
                .orElseThrow();
        assertThat(callable.variables()).extracting(ContractVariableSchemaView::variableKey)
                .containsExactly("amountInWords", "customerName", "idNumber", "letterType", "middle", "zebra");

        ContractVariableSchemaView customerName = byKey(callable, "customerName");
        assertThat(customerName.variableType()).isEqualTo(VariableType.TEXT);
        assertThat(customerName.required()).isTrue();
        assertThat(customerName.computed()).isFalse();
        assertThat(customerName.piiCategory()).isEqualTo(VariablePiiCategory.NONE);
        assertThat(customerName.enumValues()).isNull();

        ContractVariableSchemaView letterType = byKey(callable, "letterType");
        assertThat(letterType.variableType()).isEqualTo(VariableType.ENUM);
        assertThat(letterType.required()).isTrue();
        assertThat(letterType.enumValues()).containsExactly("OFFER", "REMINDER");

        ContractVariableSchemaView idNumber = byKey(callable, "idNumber");
        assertThat(idNumber.piiCategory()).isEqualTo(VariablePiiCategory.GOVERNMENT_ID);

        ContractVariableSchemaView amountInWords = byKey(callable, "amountInWords");
        assertThat(amountInWords.computed()).isTrue();
        ContractVariableSchemaView middle = byKey(callable, "middle");
        assertThat(middle.computed()).isTrue();
    }

    @Test
    void assembleOmitsInternalIdsDefaultValueAndComputeExpressionPlaintext() throws Exception {
        TemplateVersionEntity version = publishedVersion("1.0.0");
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(TEMPLATE_ID))
                .thenReturn(List.of(version));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(VERSION_ID))
                .thenReturn(List.of(
                        schema(
                                "amountInWords",
                                VariableType.COMPUTED,
                                false,
                                null,
                                "SPELL_AMOUNT(principalAmount)",
                                VariablePiiCategory.NONE
                        )
                ));

        ContractResultView contract = service.assemble(
                template,
                policy,
                "dev",
                null,
                ContractViewAudience.CALLER
        );
        JsonNode json = objectMapper.valueToTree(contract.callableVersions().getFirst().variables().getFirst());

        assertThat(json.has("id")).isFalse();
        assertThat(json.has("defaultValue")).isFalse();
        assertThat(json.has("computeExpression")).isFalse();
        assertThat(json.path("computed").asBoolean()).isTrue();
        assertThat(json.path("variableKey").asText()).isEqualTo("amountInWords");
    }

    @Test
    void listCallableVersionsOmitsVariablesWhileContractIncludesThem() {
        TemplateVersionEntity version = publishedVersion("1.0.0");
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(TEMPLATE_ID))
                .thenReturn(List.of(version));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(VERSION_ID))
                .thenReturn(List.of(
                        schema("customerName", VariableType.TEXT, true, null, null, VariablePiiCategory.NONE)
                ));

        assertThat(service.listCallableVersions(template, "dev"))
                .allSatisfy(view -> assertThat(view.variables()).isNull());

        ContractResultView contract = service.assemble(
                template,
                policy,
                "dev",
                null,
                ContractViewAudience.ADMIN
        );
        assertThat(contract.callableVersions().getFirst().variables()).isNotEmpty();
    }

    @Test
    void publishedVersionWithZeroSchemasReturnsEmptyVariablesArray() {
        TemplateVersionEntity version = publishedVersion("1.0.0");
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(TEMPLATE_ID))
                .thenReturn(List.of(version));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(VERSION_ID))
                .thenReturn(List.of());

        ContractResultView contract = service.assemble(
                template,
                policy,
                "dev",
                null,
                ContractViewAudience.CALLER
        );

        assertThat(contract.callableVersions()).hasSize(1);
        assertThat(contract.callableVersions().getFirst().variables()).isEmpty();
    }

    private static ContractVariableSchemaView byKey(CallableVersionView version, String key) {
        return version.variables().stream()
                .filter(view -> key.equals(view.variableKey()))
                .findFirst()
                .orElseThrow();
    }

    private TemplateVersionEntity publishedVersion(String releaseVersion) {
        TemplateVersionEntity version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000001");
        version.setDevVersionNumber(1);
        version.setReleaseVersion(releaseVersion);
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        return version;
    }

    private VariableSchemaEntity schema(
            String key,
            VariableType type,
            boolean required,
            String enumValues,
            String computeExpression,
            VariablePiiCategory piiCategory
    ) {
        return new VariableSchemaEntity(
                UUID.randomUUID(),
                VERSION_ID,
                key,
                type,
                required,
                "secret-default",
                enumValues,
                "desc-" + key,
                computeExpression,
                piiCategory
        );
    }
}
