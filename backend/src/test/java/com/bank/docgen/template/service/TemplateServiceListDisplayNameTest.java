package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateSummaryView;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TemplateServiceListDisplayNameTest {

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private TemplateStructuredAuthoringService structuredAuthoringService;
    @Mock
    private TemplateBindingConfigurationService bindingConfigurationService;
    @Mock
    private TemplateViewMapper templateViewMapper;
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ManagementUserDisplayService managementUserDisplayService;

    private TemplateService service;
    private ManagementSessionClaims session;

    @BeforeEach
    void setUp() {
        service = new TemplateService(
                templateRepository,
                templateVersionRepository,
                masterDocumentRepository,
                apiPolicyRepository,
                groupAccessService,
                structuredAuthoringService,
                bindingConfigurationService,
                templateViewMapper,
                templateCurrentVersionResolver,
                eventPublisher,
                managementUserDisplayService,
                org.mockito.Mockito.mock(com.bank.docgen.template.service.VariableComputeService.class),
                org.mockito.Mockito.mock(ApprovalSubStateResolver.class)
        );
        session = new ManagementSessionClaims(
                "10000001",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                List.of("GLOBAL_ADMIN"),
                List.of("*"),
                "route.home",
                List.of("route.home"),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void list_enrichesUpdatedByDisplayName() {
        UUID templateId = UUID.randomUUID();
        TemplateEntity entity = new TemplateEntity(
                templateId, "TPL-001", "RETAIL", "Loan", null, UUID.randomUUID(), "10000003");
        TemplateSummaryView summary = new TemplateSummaryView(
                templateId.toString(),
                "TPL-001",
                "RETAIL",
                "Loan",
                TemplateLifecycleStatus.DRAFT,
                ApprovalSubState.PENDING_SUBMIT,
                null,
                0,
                UUID.randomUUID().toString(),
                "10000003",
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                null);
        when(groupAccessService.accessibleGroupCodes(session)).thenReturn(List.of("*"));
        when(templateRepository.searchCatalog(any(), eq(0), eq(20)))
                .thenReturn(new CatalogQueryPage<>(List.of(entity), 1, 1));
        when(templateViewMapper.toSummary(entity)).thenReturn(summary);
        when(managementUserDisplayService.lookupDisplayNames(Set.of("10000003")))
                .thenReturn(Map.of("10000003", "Template Author (10000003)"));

        PageView<TemplateSummaryView> page = service.list(session, 0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).updatedByDisplayName()).isEqualTo("Template Author (10000003)");
    }
}
