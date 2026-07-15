package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.UpsertTestDataSetRequest;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TestDataSetEntity;
import com.bank.docgen.template.persistence.TestDataSetRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TestDataSetSchemaValidationServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private TestDataSetRepository testDataSetRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;
    @Mock
    private VariableSchemaRepository variableSchemaRepository;
    @Mock
    private ManagementAuditRecorder managementAuditRecorder;

    private TestDataSetService service;
    private UUID templateId;
    private UUID versionId;
    private ManagementSessionClaims author;
    private TemplateEntity template;
    private TemplateVersionEntity version;

    @BeforeEach
    void setUp() {
        service = new TestDataSetService(
                templateService,
                testDataSetRepository,
                groupAccessService,
                new ObjectMapper(),
                templateCurrentVersionResolver,
                variableSchemaRepository,
                managementAuditRecorder
        );
        templateId = UUID.randomUUID();
        versionId = UUID.randomUUID();
        template = new TemplateEntity(
                templateId,
                "TPL-1",
                "RETAIL",
                "Demo",
                null,
                UUID.randomUUID(),
                "10000001"
        );
        version = new TemplateVersionEntity(versionId, templateId, "10000001");
        author = session("10000003", List.of("TEMPLATE_AUTHOR"));
    }

    @Test
    void create_validVariables_persists() {
        stubMaintain();
        stubSchema(List.of(variable("customerName", VariableType.TEXT, true, null, null)));
        when(testDataSetRepository.save(any(TestDataSetEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var view = service.create(
                templateId,
                new UpsertTestDataSetRequest("Retail", null, Map.of("customerName", "Acme"), false, null, List.of()),
                author
        );

        assertThat(view.variables()).containsEntry("customerName", "Acme");
        assertThat(view.testDataSetId()).startsWith("TDS-");
    }

    @Test
    void create_missingRequired_throwsFieldErrors() {
        stubMaintain();
        stubSchema(List.of(variable("customerName", VariableType.TEXT, true, null, null)));

        assertThatThrownBy(() -> service.create(
                templateId,
                new UpsertTestDataSetRequest("Retail", null, Map.of(), false, null, List.of()),
                author
        ))
                .isInstanceOf(TestDataSetSchemaValidationException.class)
                .satisfies(ex -> {
                    TestDataSetSchemaValidationException typed = (TestDataSetSchemaValidationException) ex;
                    assertThat(typed.fieldErrors()).anySatisfy(error -> {
                        assertThat(error.field()).isEqualTo("customerName");
                        assertThat(error.reason()).isEqualTo("REQUIRED");
                    });
                });
    }

    @Test
    void create_invalidType_throwsInvalidType() {
        stubMaintain();
        stubSchema(List.of(variable("amount", VariableType.AMOUNT, false, null, null)));

        assertThatThrownBy(() -> service.create(
                templateId,
                new UpsertTestDataSetRequest("Retail", null, Map.of("amount", "not-a-number"), false, null, List.of()),
                author
        ))
                .isInstanceOf(TestDataSetSchemaValidationException.class)
                .satisfies(ex -> {
                    TestDataSetSchemaValidationException typed = (TestDataSetSchemaValidationException) ex;
                    assertThat(typed.fieldErrors()).anySatisfy(error -> {
                        assertThat(error.field()).isEqualTo("amount");
                        assertThat(error.reason()).isEqualTo("INVALID_TYPE");
                    });
                });
    }

    @Test
    void create_enumNotAllowed_throws() {
        stubMaintain();
        stubSchema(List.of(variable("status", VariableType.ENUM, false, null, "ACTIVE,CLOSED")));

        assertThatThrownBy(() -> service.create(
                templateId,
                new UpsertTestDataSetRequest("Retail", null, Map.of("status", "NOPE"), false, null, List.of()),
                author
        ))
                .isInstanceOf(TestDataSetSchemaValidationException.class)
                .satisfies(ex -> {
                    TestDataSetSchemaValidationException typed = (TestDataSetSchemaValidationException) ex;
                    assertThat(typed.fieldErrors()).anySatisfy(error ->
                            assertThat(error.reason()).isEqualTo("ENUM_NOT_ALLOWED"));
                });
    }

    @Test
    void create_unknownField_throws() {
        stubMaintain();
        stubSchema(List.of(variable("customerName", VariableType.TEXT, true, null, null)));

        assertThatThrownBy(() -> service.create(
                templateId,
                new UpsertTestDataSetRequest(
                        "Retail",
                        null,
                        Map.of("customerName", "Acme", "extraHack", 1),
                        false,
                        null,
                        List.of()
                ),
                author
        ))
                .isInstanceOf(TestDataSetSchemaValidationException.class)
                .satisfies(ex -> {
                    TestDataSetSchemaValidationException typed = (TestDataSetSchemaValidationException) ex;
                    assertThat(typed.fieldErrors()).anySatisfy(error -> {
                        assertThat(error.field()).isEqualTo("extraHack");
                        assertThat(error.reason()).isEqualTo("UNKNOWN_FIELD");
                    });
                });
    }

    @Test
    void create_missingComputeField_succeedsAndStripsExplicitComputeKey() {
        stubMaintain();
        stubSchema(List.of(
                variable("principal", VariableType.AMOUNT, true, null, null),
                variable("principalCn", VariableType.COMPUTED, true, null, null)
        ));
        when(testDataSetRepository.save(any(TestDataSetEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var view = service.create(
                templateId,
                new UpsertTestDataSetRequest(
                        "Retail",
                        null,
                        Map.of("principal", 100, "principalCn", "ignored"),
                        false,
                        null,
                        List.of()
                ),
                author
        );

        ArgumentCaptor<TestDataSetEntity> captor = ArgumentCaptor.forClass(TestDataSetEntity.class);
        verify(testDataSetRepository).save(captor.capture());
        assertThat(captor.getValue().getVariablesJson()).doesNotContain("principalCn");
        assertThat(view.variables()).containsEntry("principal", 100);
        assertThat(view.variables()).doesNotContainKey("principalCn");
    }

    private void stubMaintain() {
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
    }

    private void stubSchema(List<VariableSchemaEntity> schema) {
        when(templateCurrentVersionResolver.findInFlightDevVersion(templateId)).thenReturn(Optional.of(version));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId)).thenReturn(schema);
    }

    private VariableSchemaEntity variable(
            String key,
            VariableType type,
            boolean required,
            String defaultValue,
            String enumValues
    ) {
        return new VariableSchemaEntity(
                UUID.randomUUID(),
                versionId,
                key,
                type,
                required,
                defaultValue,
                enumValues,
                null,
                type == VariableType.COMPUTED ? "${principal}" : null
        );
    }

    private ManagementSessionClaims session(String username, List<String> roles) {
        return new ManagementSessionClaims(
                username,
                username,
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
    }
}
