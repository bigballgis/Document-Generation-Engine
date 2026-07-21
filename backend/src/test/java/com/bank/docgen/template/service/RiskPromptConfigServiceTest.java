package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.DecisionFormConfigView;
import com.bank.docgen.template.api.RiskPromptConfigView;
import com.bank.docgen.template.api.TemplateRiskPromptConfigView;
import com.bank.docgen.template.api.UpsertGlobalRiskPromptConfigRequest;
import com.bank.docgen.template.api.UpsertTemplateRiskPromptConfigRequest;
import com.bank.docgen.template.domain.RiskPromptScope;
import com.bank.docgen.template.persistence.RiskPromptConfigEntity;
import com.bank.docgen.template.persistence.RiskPromptConfigRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRiskPromptOverrideEntity;
import com.bank.docgen.template.persistence.TemplateRiskPromptOverrideRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RiskPromptConfigServiceTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");

    @Mock
    private RiskPromptConfigRepository riskPromptConfigRepository;
    @Mock
    private TemplateRiskPromptOverrideRepository templateRiskPromptOverrideRepository;
    @Mock
    private TemplateService templateService;
    @Mock
    private ManagementAuditRecorder auditRecorder;

    private RiskPromptConfigService service;
    private ManagementSessionClaims globalAdmin;
    private ManagementSessionClaims groupAuthor;
    private ManagementSessionClaims crossGroupActor;
    private TemplateEntity retailTemplate;

    @BeforeEach
    void setUp() {
        service = new RiskPromptConfigService(
                riskPromptConfigRepository,
                templateRiskPromptOverrideRepository,
                templateService,
                auditRecorder,
                new ObjectMapper()
        );
        globalAdmin = session("10000001", "Global Admin", List.of("GLOBAL_ADMIN"), List.of("*"));
        groupAuthor = session("10000002", "Group Author", List.of("DOCUMENT_AUTHOR"), List.of("RETAIL"));
        crossGroupActor = session("10000003", "Corp Author", List.of("DOCUMENT_AUTHOR"), List.of("CORP"));
        retailTemplate = template(TEMPLATE_ID, "RETAIL");
    }

    @Test
    void s1_noTemplateOverride_inheritsGlobalCategories() {
        RiskPromptConfigEntity global = globalEntity(allCategories());
        when(templateService.requireReadableTemplate(TEMPLATE_ID, groupAuthor)).thenReturn(retailTemplate);
        when(templateRiskPromptOverrideRepository.findById(TEMPLATE_ID)).thenReturn(Optional.empty());
        when(riskPromptConfigRepository.findByScopeTypeAndGroupCode(RiskPromptScope.GLOBAL, null))
                .thenReturn(Optional.of(global));

        TemplateRiskPromptConfigView view = service.getTemplateConfig(TEMPLATE_ID, groupAuthor);

        assertThat(view.useDefault()).isTrue();
        assertThat(view.reasonCategories()).containsExactlyElementsOf(allCategories());
    }

    @Test
    void s2_templateOverride_takesPrecedenceForDecisionResolve() {
        TemplateRiskPromptOverrideEntity override = overrideEntity(
                List.of("FIDELITY_WARNING", "OTHER"),
                Map.of("UNRESOLVED_BLOCKERS", "Template-specific copy.")
        );
        when(templateService.requireReadableTemplate(TEMPLATE_ID, groupAuthor)).thenReturn(retailTemplate);
        when(templateRiskPromptOverrideRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(override));

        DecisionFormConfigView view = service.resolveDecisionFormConfig(TEMPLATE_ID, groupAuthor);

        assertThat(view.reasonCategories()).containsExactly("FIDELITY_WARNING", "OTHER");
        assertThat(view.riskPromptCopy()).containsEntry("UNRESOLVED_BLOCKERS", "Template-specific copy.");
    }

    @Test
    void s3_createWithoutOverride_hasNoOverrideRow() {
        when(templateService.requireReadableTemplate(TEMPLATE_ID, groupAuthor)).thenReturn(retailTemplate);
        when(templateRiskPromptOverrideRepository.findById(TEMPLATE_ID)).thenReturn(Optional.empty());
        when(riskPromptConfigRepository.findByScopeTypeAndGroupCode(RiskPromptScope.GLOBAL, null))
                .thenReturn(Optional.of(globalEntity(allCategories())));

        TemplateRiskPromptConfigView view = service.getTemplateConfig(TEMPLATE_ID, groupAuthor);

        assertThat(view.useDefault()).isTrue();
        verify(templateRiskPromptOverrideRepository, never()).save(any());
    }

    @Test
    void s5_inheritGlobal_clearsOverrideRow() {
        TemplateRiskPromptOverrideEntity existing = overrideEntity(List.of("OTHER"), Map.of());
        when(templateService.requireWritableTemplate(TEMPLATE_ID, groupAuthor)).thenReturn(retailTemplate);
        when(templateRiskPromptOverrideRepository.findById(TEMPLATE_ID))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.empty());
        when(riskPromptConfigRepository.findByScopeTypeAndGroupCode(RiskPromptScope.GLOBAL, null))
                .thenReturn(Optional.of(globalEntity(allCategories())));

        UpsertTemplateRiskPromptConfigRequest request = new UpsertTemplateRiskPromptConfigRequest(
                true,
                null,
                null
        );

        TemplateRiskPromptConfigView view = service.upsertTemplateConfig(TEMPLATE_ID, request, groupAuthor);

        verify(templateRiskPromptOverrideRepository).delete(existing);
        assertThat(view.useDefault()).isTrue();
        assertThat(view.reasonCategories()).containsExactlyElementsOf(allCategories());
    }

    @Test
    void s6_globalUpdate_affectsTemplatesWithoutOverride() {
        RiskPromptConfigEntity global = globalEntity(List.of("BINDING_ISSUE", "OTHER"));
        when(riskPromptConfigRepository.findByScopeTypeAndGroupCode(RiskPromptScope.GLOBAL, null))
                .thenReturn(Optional.of(global));
        when(riskPromptConfigRepository.save(any(RiskPromptConfigEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpsertGlobalRiskPromptConfigRequest request = new UpsertGlobalRiskPromptConfigRequest(
                List.of("BINDING_ISSUE", "OTHER"),
                Map.of("UNRESOLVED_BLOCKERS", "Updated global copy.")
        );

        RiskPromptConfigView view = service.upsertGlobal(request, globalAdmin);

        assertThat(view.reasonCategories()).containsExactly("BINDING_ISSUE", "OTHER");
        verify(auditRecorder).recordRiskPromptConfigUpdated(
                eq("GLOBAL"),
                eq(null),
                eq("10000001"),
                eq("Global Admin"),
                any(String.class)
        );
    }

    @Test
    void s9_crossGroupAccess_isDenied() {
        when(templateService.requireReadableTemplate(TEMPLATE_ID, crossGroupActor))
                .thenThrow(new TemplateAccessDeniedException());

        assertThatThrownBy(() -> service.getTemplateConfig(TEMPLATE_ID, crossGroupActor))
                .isInstanceOf(TemplateAccessDeniedException.class);
    }

    @Test
    void s10_useDefaultFalseWithEmptyCategories_isValidationError() {
        when(templateService.requireWritableTemplate(TEMPLATE_ID, groupAuthor)).thenReturn(retailTemplate);

        UpsertTemplateRiskPromptConfigRequest request = new UpsertTemplateRiskPromptConfigRequest(
                false,
                List.of(),
                Map.of()
        );

        assertThatThrownBy(() -> service.upsertTemplateConfig(TEMPLATE_ID, request, groupAuthor))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.validation.requestBodyInvalid");
    }

    @Test
    void templateOverrideSave_isAudited() {
        TemplateRiskPromptOverrideEntity saved = overrideEntity(
                List.of("COVERAGE_BELOW_THRESHOLD"),
                Map.of("BELOW_THRESHOLD_COVERAGE", "Custom threshold copy.")
        );
        when(templateService.requireWritableTemplate(TEMPLATE_ID, groupAuthor)).thenReturn(retailTemplate);
        when(templateRiskPromptOverrideRepository.findById(TEMPLATE_ID))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(saved));
        when(templateRiskPromptOverrideRepository.save(any(TemplateRiskPromptOverrideEntity.class)))
                .thenReturn(saved);

        UpsertTemplateRiskPromptConfigRequest request = new UpsertTemplateRiskPromptConfigRequest(
                false,
                List.of("COVERAGE_BELOW_THRESHOLD"),
                Map.of("BELOW_THRESHOLD_COVERAGE", "Custom threshold copy.")
        );

        service.upsertTemplateConfig(TEMPLATE_ID, request, groupAuthor);

        verify(auditRecorder).recordRiskPromptConfigUpdated(
                eq("TEMPLATE"),
                eq("RETAIL"),
                eq("10000002"),
                eq("Group Author"),
                eq("templateId=" + TEMPLATE_ID + ",reasonCategories=1")
        );
    }

    @Test
    void globalUpsert_requiresGlobalAdmin() {
        UpsertGlobalRiskPromptConfigRequest request = new UpsertGlobalRiskPromptConfigRequest(
                List.of("BINDING_ISSUE"),
                Map.of("UNRESOLVED_BLOCKERS", "Review blockers.")
        );

        assertThatThrownBy(() -> service.upsertGlobal(request, groupAuthor))
                .isInstanceOf(TemplateAccessDeniedException.class);
    }

    private List<String> allCategories() {
        return List.of(
                "BINDING_ISSUE",
                "VARIABLE_SCHEMA_ISSUE",
                "RULE_VALIDATION_ISSUE",
                "FIDELITY_WARNING",
                "COVERAGE_BELOW_THRESHOLD",
                "PREVIEW_COMPARISON_DIFF",
                "CONTRACT_SCOPE_CHANGE",
                "OTHER"
        );
    }

    private RiskPromptConfigEntity globalEntity(List<String> categories) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return new RiskPromptConfigEntity(
                    UUID.randomUUID(),
                    RiskPromptScope.GLOBAL,
                    null,
                    mapper.writeValueAsString(categories),
                    mapper.writeValueAsString(Map.of("UNRESOLVED_BLOCKERS", "Review blockers."))
            );
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private TemplateRiskPromptOverrideEntity overrideEntity(List<String> categories, Map<String, String> copy) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return new TemplateRiskPromptOverrideEntity(
                    TEMPLATE_ID,
                    mapper.writeValueAsString(categories),
                    mapper.writeValueAsString(copy)
            );
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private TemplateEntity template(UUID id, String groupCode) {
        return new TemplateEntity(
                id,
                "tpl-" + id,
                groupCode,
                "Template",
                "Description",
                UUID.randomUUID(),
                "10000002"
        );
    }

    private ManagementSessionClaims session(
            String username,
            String displayName,
            List<String> roles,
            List<String> groups
    ) {
        return new ManagementSessionClaims(
                username,
                displayName,
                username + "@example.com",
                AuthSource.LOCAL,
                roles,
                groups,
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }
}
