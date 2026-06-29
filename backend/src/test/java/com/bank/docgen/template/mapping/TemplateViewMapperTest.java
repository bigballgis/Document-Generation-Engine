package com.bank.docgen.template.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.template.api.AnchorBindingView;
import com.bank.docgen.template.api.CompositionRuleView;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.TemplateSummaryView;
import com.bank.docgen.template.api.VariableSchemaView;
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.template.service.ApprovalSubStateResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateViewMapperTest {

    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private VariableSchemaRepository variableSchemaRepository;
    @Mock
    private AnchorBindingRepository anchorBindingRepository;
    @Mock
    private ApprovalSubStateResolver approvalSubStateResolver;

    private TemplateViewMapper mapper;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mapper = new TemplateViewMapper(
                templateVersionRepository,
                variableSchemaRepository,
                anchorBindingRepository,
                approvalSubStateResolver,
                objectMapper
        );
    }

    @Test
    void toVariableView_mapsEntityFields() {
        UUID variableId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        VariableSchemaEntity entity = new VariableSchemaEntity(
                variableId,
                versionId,
                "customerName",
                VariableType.TEXT,
                true,
                "default",
                null,
                "Customer display name"
        );

        VariableSchemaView view = mapper.toVariableView(entity);

        assertThat(view.id()).isEqualTo(variableId.toString());
        assertThat(view.variableKey()).isEqualTo("customerName");
        assertThat(view.variableType()).isEqualTo(VariableType.TEXT);
        assertThat(view.required()).isTrue();
        assertThat(view.defaultValue()).isEqualTo("default");
        assertThat(view.description()).isEqualTo("Customer display name");
    }

    @Test
    void toBindingView_mapsEntityFields() {
        UUID bindingId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        AnchorBindingEntity entity = new AnchorBindingEntity(
                bindingId,
                versionId,
                "BODY_1",
                AnchorContentType.TEXT,
                "{\"type\":\"paragraph\"}",
                BindingValidationStatus.VALID
        );

        AnchorBindingView view = mapper.toBindingView(entity);

        assertThat(view.id()).isEqualTo(bindingId.toString());
        assertThat(view.anchorId()).isEqualTo("BODY_1");
        assertThat(view.declaredContentType()).isEqualTo("TEXT");
        assertThat(view.structuredContentJson()).isEqualTo("{\"type\":\"paragraph\"}");
        assertThat(view.validationStatus()).isEqualTo(BindingValidationStatus.VALID);
    }

    @Test
    void toSummary_countsReleaseVersions() {
        UUID templateId = UUID.randomUUID();
        TemplateEntity template = new TemplateEntity(
                templateId,
                "TPL-001",
                "RETAIL",
                "Loan template",
                "desc",
                UUID.randomUUID(),
                "10000001"
        );
        template.setReleaseVersion("v1.0.0");
        TemplateVersionEntity withRelease = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000001");
        withRelease.setReleaseVersion("v1.0.0");
        TemplateVersionEntity withoutRelease = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000001");
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(withRelease, withoutRelease));

        TemplateSummaryView view = mapper.toSummary(template);

        assertThat(view.id()).isEqualTo(templateId.toString());
        assertThat(view.externalId()).isEqualTo("TPL-001");
        assertThat(view.releaseVersionCount()).isEqualTo(1);
        assertThat(view.releaseVersion()).isEqualTo("v1.0.0");
    }

    @Test
    void toDetail_assemblesVariablesBindingsAndRules() throws Exception {
        UUID templateId = UUID.randomUUID();
        UUID masterId = UUID.randomUUID();
        TemplateEntity template = new TemplateEntity(
                templateId,
                "TPL-002",
                "RETAIL",
                "Detail template",
                "description",
                masterId,
                "10000002"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        UUID versionId = UUID.randomUUID();
        TemplateVersionEntity version = new TemplateVersionEntity(versionId, templateId, "10000002");
        version.setRulesJson(objectMapper.writeValueAsString(List.of(
                new CompositionRuleView("rule-1", "x > 0", "ANCHOR_A", null, null)
        )));
        VariableSchemaEntity variable = new VariableSchemaEntity(
                UUID.randomUUID(),
                versionId,
                "amount",
                VariableType.NUMBER,
                false,
                null,
                null,
                null
        );
        AnchorBindingEntity binding = new AnchorBindingEntity(
                UUID.randomUUID(),
                versionId,
                "ANCHOR_A",
                AnchorContentType.TEXT,
                "{}",
                BindingValidationStatus.VALID
        );
        when(templateVersionRepository.findByTemplateIdAndDevVersionNumber(templateId, 1))
                .thenReturn(Optional.of(version));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId))
                .thenReturn(List.of(variable));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId))
                .thenReturn(List.of(binding));

        TemplateDetailView detail = mapper.toDetail(template);

        assertThat(detail.id()).isEqualTo(templateId.toString());
        assertThat(detail.masterId()).isEqualTo(masterId.toString());
        assertThat(detail.variables()).hasSize(1);
        assertThat(detail.variables().getFirst().variableKey()).isEqualTo("amount");
        assertThat(detail.bindings()).hasSize(1);
        assertThat(detail.bindings().getFirst().anchorId()).isEqualTo("ANCHOR_A");
        assertThat(detail.rules()).hasSize(1);
        assertThat(detail.rules().getFirst().ruleId()).isEqualTo("rule-1");
        assertThat(detail.approvalSubState()).isNull();
    }

    @Test
    void toDetail_resolvesApprovalSubStateWhenPendingDecision() {
        UUID templateId = UUID.randomUUID();
        TemplateEntity template = new TemplateEntity(
                templateId,
                "TPL-003",
                "RETAIL",
                "Approval template",
                null,
                UUID.randomUUID(),
                "10000003"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.APPROVAL);
        UUID versionId = UUID.randomUUID();
        TemplateVersionEntity version = new TemplateVersionEntity(versionId, templateId, "10000003");
        when(templateVersionRepository.findByTemplateIdAndDevVersionNumber(templateId, 1))
                .thenReturn(Optional.of(version));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId))
                .thenReturn(List.of());
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId))
                .thenReturn(List.of());
        when(approvalSubStateResolver.resolve(template))
                .thenReturn(ApprovalSubState.PENDING_DECISION);

        TemplateDetailView detail = mapper.toDetail(template);

        assertThat(detail.approvalSubState()).isEqualTo(ApprovalSubState.PENDING_DECISION);
    }

    @Test
    void loadRules_returnsEmptyForBlankJson() {
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), UUID.randomUUID(), "10000004");
        version.setRulesJson("  ");

        assertThat(mapper.loadRules(version)).isEmpty();
    }

    @Test
    void loadRules_parsesJson() throws Exception {
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), UUID.randomUUID(), "10000005");
        version.setRulesJson(objectMapper.writeValueAsString(List.of(
                new CompositionRuleView("r1", "cond", "A1", null, null)
        )));

        List<CompositionRuleView> rules = mapper.loadRules(version);

        assertThat(rules).hasSize(1);
        assertThat(rules.getFirst().ruleId()).isEqualTo("r1");
    }
}
