package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.UpsertTestDataSetRequest;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TestDataSetEntity;
import com.bank.docgen.template.persistence.TestDataSetRepository;
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
class TestDataSetGovernanceServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private TestDataSetRepository testDataSetRepository;
    @Mock
    private GroupAccessService groupAccessService;

    private TestDataSetService service;
    private UUID templateId;
    private ManagementSessionClaims author;
    private ManagementSessionClaims tester;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        service = new TestDataSetService(
                templateService,
                testDataSetRepository,
                groupAccessService,
                new ObjectMapper()
        );
        templateId = UUID.randomUUID();
        template = new TemplateEntity(
                templateId,
                "TPL-1",
                "RETAIL",
                "Demo",
                null,
                UUID.randomUUID(),
                "10000001"
        );
        author = session("10000003", List.of("TEMPLATE_AUTHOR"));
        tester = session("10000006", List.of("TEMPLATE_TESTER"));
    }

    @Test
    void create_withRequiredFlagAndScenario_persists() {
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        when(testDataSetRepository.save(any(TestDataSetEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var view = service.create(
                templateId,
                new UpsertTestDataSetRequest(
                        "Retail sample",
                        "Synthetic customer",
                        Map.of("customerName", "DatasetCustomer"),
                        true,
                        "Happy path",
                        List.of("VAR_REQUIRED", "ANCHOR_HEADER")
                ),
                author
        );

        ArgumentCaptor<TestDataSetEntity> captor = ArgumentCaptor.forClass(TestDataSetEntity.class);
        verify(testDataSetRepository).save(captor.capture());
        TestDataSetEntity saved = captor.getValue();
        assertThat(saved.isRequired()).isTrue();
        assertThat(saved.getScenarioName()).isEqualTo("Happy path");
        assertThat(saved.getCoverageTagsJson()).contains("VAR_REQUIRED");
        assertThat(saved.getDatasetVersion()).isEqualTo(1);
        assertThat(saved.isLocked()).isFalse();
        assertThat(view.required()).isTrue();
        assertThat(view.scenarioName()).isEqualTo("Happy path");
        assertThat(view.coverageTags()).containsExactly("VAR_REQUIRED", "ANCHOR_HEADER");
    }

    @Test
    void update_lockedDataSet_throwsImmutable() {
        TestDataSetEntity locked = dataSet("TDS-LOCKED", true);
        locked.lockForEvidence();
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        when(testDataSetRepository.findByTemplateIdAndExternalId(templateId, "TDS-LOCKED"))
                .thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> service.update(
                templateId,
                "TDS-LOCKED",
                new UpsertTestDataSetRequest("New name", null, Map.of(), false, null, List.of()),
                author
        ))
                .isInstanceOf(TestDataSetImmutableException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.template.testDataSetLocked");
    }

    @Test
    void derive_fromLocked_createsNewVersion_unlocked() {
        TestDataSetEntity locked = dataSet("TDS-LOCKED", true);
        locked.lockForEvidence();
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        when(testDataSetRepository.findByTemplateIdAndExternalId(templateId, "TDS-LOCKED"))
                .thenReturn(Optional.of(locked));
        when(testDataSetRepository.save(any(TestDataSetEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var derived = service.derive(templateId, "TDS-LOCKED", author);

        ArgumentCaptor<TestDataSetEntity> captor = ArgumentCaptor.forClass(TestDataSetEntity.class);
        verify(testDataSetRepository).save(captor.capture());
        TestDataSetEntity saved = captor.getValue();
        assertThat(saved.isLocked()).isFalse();
        assertThat(saved.getDatasetVersion()).isEqualTo(2);
        assertThat(saved.getDerivedFromId()).isEqualTo(locked.getId());
        assertThat(derived.locked()).isFalse();
        assertThat(derived.datasetVersion()).isEqualTo(2);
        assertThat(derived.testDataSetId()).startsWith("TDS-");
        assertThat(derived.testDataSetId()).isNotEqualTo("TDS-LOCKED");
    }

    @Test
    void maintain_byTesterRole_throwsAccessDenied() {
        when(templateService.requireReadableTemplate(templateId, tester)).thenReturn(template);
        when(groupAccessService.canAuthorTemplates(tester)).thenReturn(false);

        assertThatThrownBy(() -> service.create(
                templateId,
                new UpsertTestDataSetRequest("Retail sample", null, Map.of("k", "v"), false, null, List.of()),
                tester
        )).isInstanceOf(TemplateAccessDeniedException.class);
    }

    private TestDataSetEntity dataSet(String externalId, boolean required) {
        TestDataSetEntity entity = new TestDataSetEntity(
                UUID.randomUUID(),
                templateId,
                externalId,
                "Retail sample",
                "desc",
                "{\"customerName\":\"DatasetCustomer\"}",
                required,
                "Happy path",
                "[\"VAR_REQUIRED\"]",
                1,
                false,
                null
        );
        return entity;
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
