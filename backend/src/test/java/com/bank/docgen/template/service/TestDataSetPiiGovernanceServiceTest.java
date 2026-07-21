package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.UpsertTestDataSetRequest;
import com.bank.docgen.template.domain.TestDataSetPiiHandling;
import com.bank.docgen.template.domain.VariablePiiCategory;
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
class TestDataSetPiiGovernanceServiceTest {

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
        author = session("10000003", List.of("DOCUMENT_AUTHOR"));
    }

    @Test
    void create_nonPiiSchema_withoutHandling_succeeds() {
        stubMaintain();
        stubSchema(List.of(variable("note", VariableType.TEXT, false, VariablePiiCategory.NONE)));
        when(testDataSetRepository.save(any(TestDataSetEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var view = service.create(
                templateId,
                new UpsertTestDataSetRequest("Retail", null, Map.of("note", "ok"), false, null, List.of()),
                author
        );

        assertThat(view.variables()).containsEntry("note", "ok");
        verify(managementAuditRecorder, never()).recordTestDataPiiExplicitConfirm(
                any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void create_optionalPiiFieldOmitted_withoutHandling_succeeds() {
        stubMaintain();
        stubSchema(List.of(
                variable("customerName", VariableType.TEXT, false, VariablePiiCategory.PERSONAL_NAME),
                variable("productCode", VariableType.TEXT, true, VariablePiiCategory.NONE)
        ));
        when(testDataSetRepository.save(any(TestDataSetEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var view = service.create(
                templateId,
                new UpsertTestDataSetRequest(
                        "Retail", null, Map.of("productCode", "P-1"), false, null, List.of()),
                author
        );

        assertThat(view.variables()).containsEntry("productCode", "P-1");
        assertThat(view.variables()).doesNotContainKey("customerName");
    }

    @Test
    void create_piiValueWithSynthetic_persists() {
        stubMaintain();
        stubSchema(List.of(variable("customerName", VariableType.TEXT, true, VariablePiiCategory.PERSONAL_NAME)));
        when(testDataSetRepository.save(any(TestDataSetEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var view = service.create(
                templateId,
                new UpsertTestDataSetRequest(
                        "Retail",
                        null,
                        Map.of("customerName", "Synthetic Person"),
                        false,
                        null,
                        List.of(),
                        TestDataSetPiiHandling.SYNTHETIC.name(),
                        null,
                        null
                ),
                author
        );

        assertThat(view.variables()).containsEntry("customerName", "Synthetic Person");
        verify(managementAuditRecorder, never()).recordTestDataPiiExplicitConfirm(
                any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void create_piiValueWithExplicitSensitive_persistsAndAuditsWithoutPlaintext() {
        stubMaintain();
        stubSchema(List.of(variable("customerName", VariableType.TEXT, true, VariablePiiCategory.PERSONAL_NAME)));
        when(testDataSetRepository.save(any(TestDataSetEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        String secretValue = "RealCustomer-SECRET-VALUE";
        var view = service.create(
                templateId,
                new UpsertTestDataSetRequest(
                        "Retail",
                        null,
                        Map.of("customerName", secretValue),
                        false,
                        null,
                        List.of(),
                        TestDataSetPiiHandling.EXPLICIT_SENSITIVE.name(),
                        "Approved synthetic-like fixture for QA",
                        true
                ),
                author
        );

        assertThat(view.variables()).containsEntry("customerName", secretValue);

        ArgumentCaptor<List<String>> keysCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<Map<String, String>> categoriesCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<String> reasonCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(managementAuditRecorder).recordTestDataPiiExplicitConfirm(
                eq(templateId),
                eq("RETAIL"),
                eq(view.testDataSetId()),
                eq(1),
                hashCaptor.capture(),
                keysCaptor.capture(),
                categoriesCaptor.capture(),
                reasonCaptor.capture(),
                eq(author.username()),
                eq(author.displayName())
        );
        assertThat(keysCaptor.getValue()).containsExactly("customerName");
        assertThat(categoriesCaptor.getValue()).containsEntry("customerName", "PERSONAL_NAME");
        assertThat(reasonCaptor.getValue()).isEqualTo("Approved synthetic-like fixture for QA");
        assertThat(hashCaptor.getValue()).hasSize(64);
        assertThat(hashCaptor.getValue()).doesNotContain(secretValue);
        assertThat(reasonCaptor.getValue()).doesNotContain(secretValue);
    }

    @Test
    void create_piiValueMissingHandling_throwsAndDoesNotPersist() {
        stubMaintain();
        stubSchema(List.of(variable("customerName", VariableType.TEXT, true, VariablePiiCategory.PERSONAL_NAME)));

        assertThatThrownBy(() -> service.create(
                templateId,
                new UpsertTestDataSetRequest(
                        "Retail", null, Map.of("customerName", "Acme"), false, null, List.of()),
                author
        ))
                .isInstanceOf(TemplateValidationException.class)
                .extracting(ex -> ((TemplateValidationException) ex).messageKey())
                .isEqualTo("api.error.template.testDataSetPiiHandlingRequired");

        verify(testDataSetRepository, never()).save(any());
        verify(managementAuditRecorder, never()).recordTestDataPiiExplicitConfirm(
                any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void create_explicitSensitiveMissingReason_throwsAndDoesNotPersist() {
        stubMaintain();
        stubSchema(List.of(variable("customerName", VariableType.TEXT, true, VariablePiiCategory.PERSONAL_NAME)));

        assertThatThrownBy(() -> service.create(
                templateId,
                new UpsertTestDataSetRequest(
                        "Retail",
                        null,
                        Map.of("customerName", "Acme"),
                        false,
                        null,
                        List.of(),
                        TestDataSetPiiHandling.EXPLICIT_SENSITIVE.name(),
                        "   ",
                        true
                ),
                author
        ))
                .isInstanceOf(TemplateValidationException.class)
                .extracting(ex -> ((TemplateValidationException) ex).messageKey())
                .isEqualTo("api.error.template.piiConfirmReasonRequired");

        verify(testDataSetRepository, never()).save(any());
    }

    @Test
    void create_explicitSensitiveMissingSecondaryConfirm_throwsAndDoesNotPersist() {
        stubMaintain();
        stubSchema(List.of(variable("customerName", VariableType.TEXT, true, VariablePiiCategory.PERSONAL_NAME)));

        assertThatThrownBy(() -> service.create(
                templateId,
                new UpsertTestDataSetRequest(
                        "Retail",
                        null,
                        Map.of("customerName", "Acme"),
                        false,
                        null,
                        List.of(),
                        TestDataSetPiiHandling.EXPLICIT_SENSITIVE.name(),
                        "Need realistic fixture",
                        false
                ),
                author
        ))
                .isInstanceOf(TemplateValidationException.class)
                .extracting(ex -> ((TemplateValidationException) ex).messageKey())
                .isEqualTo("api.error.template.piiSecondaryConfirmRequired");

        verify(testDataSetRepository, never()).save(any());
    }

    @Test
    void create_illegalPiiHandling_throws() {
        stubMaintain();
        stubSchema(List.of(variable("customerName", VariableType.TEXT, true, VariablePiiCategory.PERSONAL_NAME)));

        assertThatThrownBy(() -> service.create(
                templateId,
                new UpsertTestDataSetRequest(
                        "Retail",
                        null,
                        Map.of("customerName", "Acme"),
                        false,
                        null,
                        List.of(),
                        "MASKED_SILENT",
                        null,
                        null
                ),
                author
        ))
                .isInstanceOf(TemplateValidationException.class)
                .extracting(ex -> ((TemplateValidationException) ex).messageKey())
                .isEqualTo("api.error.template.testDataSetPiiHandlingRequired");
    }

    @Test
    void update_lockedSet_immutableBeforePiiGate() {
        TestDataSetEntity locked = dataSet("TDS-LOCKED");
        locked.lockForEvidence();
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        when(testDataSetRepository.findByTemplateIdAndExternalId(templateId, "TDS-LOCKED"))
                .thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> service.update(
                templateId,
                "TDS-LOCKED",
                new UpsertTestDataSetRequest(
                        "New name",
                        null,
                        Map.of("customerName", "Acme"),
                        false,
                        null,
                        List.of(),
                        TestDataSetPiiHandling.SYNTHETIC.name(),
                        null,
                        null
                ),
                author
        ))
                .isInstanceOf(TestDataSetImmutableException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.template.testDataSetLocked");

        verify(variableSchemaRepository, never()).findByTemplateVersionIdOrderByVariableKeyAsc(any());
        verify(managementAuditRecorder, never()).recordTestDataPiiExplicitConfirm(
                any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void update_piiValueWithSynthetic_persists() {
        stubMaintain();
        stubSchema(List.of(variable("customerName", VariableType.TEXT, true, VariablePiiCategory.PERSONAL_NAME)));
        TestDataSetEntity existing = dataSet("TDS-UPD");
        when(testDataSetRepository.findByTemplateIdAndExternalId(templateId, "TDS-UPD"))
                .thenReturn(Optional.of(existing));
        when(testDataSetRepository.save(any(TestDataSetEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        var view = service.update(
                templateId,
                "TDS-UPD",
                new UpsertTestDataSetRequest(
                        "Retail",
                        null,
                        Map.of("customerName", "Synthetic Person"),
                        false,
                        null,
                        List.of(),
                        TestDataSetPiiHandling.SYNTHETIC.name(),
                        null,
                        null
                ),
                author
        );

        assertThat(view.variables()).containsEntry("customerName", "Synthetic Person");
        verify(managementAuditRecorder, never()).recordTestDataPiiExplicitConfirm(
                any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void create_explicitSensitive_auditFailure_propagatesFailClosed() {
        stubMaintain();
        stubSchema(List.of(variable("customerName", VariableType.TEXT, true, VariablePiiCategory.PERSONAL_NAME)));
        when(testDataSetRepository.save(any(TestDataSetEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new IllegalStateException("audit writer unavailable")).when(managementAuditRecorder)
                .recordTestDataPiiExplicitConfirm(
                        any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any()
                );

        assertThatThrownBy(() -> service.create(
                templateId,
                new UpsertTestDataSetRequest(
                        "Retail",
                        null,
                        Map.of("customerName", "Acme"),
                        false,
                        null,
                        List.of(),
                        TestDataSetPiiHandling.EXPLICIT_SENSITIVE.name(),
                        "Need realistic fixture",
                        true
                ),
                author
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("audit writer unavailable");
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
            VariablePiiCategory piiCategory
    ) {
        return new VariableSchemaEntity(
                UUID.randomUUID(),
                versionId,
                key,
                type,
                required,
                null,
                null,
                null,
                null,
                piiCategory
        );
    }

    private TestDataSetEntity dataSet(String externalId) {
        return new TestDataSetEntity(
                UUID.randomUUID(),
                templateId,
                externalId,
                "Retail sample",
                "desc",
                "{\"customerName\":\"DatasetCustomer\"}",
                true,
                "Happy path",
                "[\"VAR_REQUIRED\"]",
                1,
                false,
                null
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
