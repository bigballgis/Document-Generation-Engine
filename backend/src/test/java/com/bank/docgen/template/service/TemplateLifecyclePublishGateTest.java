package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.BindingValidationSummaryView;
import com.bank.docgen.template.api.BindingValidationView;
import com.bank.docgen.template.api.PublishTemplateRequest;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateLifecyclePublishGateTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private TemplateLifecycleRecordRepository lifecycleRecordRepository;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private LifecycleImpactPreviewService lifecycleImpactPreviewService;
    @Mock
    private MessageResolver messageResolver;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;

    private TemplateLifecycleService service;
    private ManagementSessionClaims groupAdmin;
    private UUID templateId;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        service = new TemplateLifecycleService(
                templateService,
                templateRepository,
                templateVersionRepository,
                lifecycleRecordRepository,
                groupAccessService,
                lifecycleImpactPreviewService,
                messageResolver,
                apiPolicyRepository
        );
        groupAdmin = new ManagementSessionClaims(
                "10000002",
                "Group Admin",
                "group.admin@example.com",
                AuthSource.LOCAL,
                List.of("GROUP_ADMIN"),
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
        templateId = UUID.randomUUID();
        template = new TemplateEntity(
                templateId,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000002"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PENDING_RELEASE);
    }

    @Test
    void publishBlockedWhenBindingsHaveBlockingIssues() {
        when(groupAccessService.canPublishTemplates(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(templateService.validateBindings(templateId, groupAdmin)).thenReturn(blockingBindings());

        assertThatThrownBy(() -> service.publish(templateId, new PublishTemplateRequest("1.0.0"), groupAdmin))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.template.publishGateBlocked");
    }

    @Test
    void publishBlockedWhenApiPolicyMissing() {
        when(groupAccessService.canPublishTemplates(groupAdmin)).thenReturn(true);
        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        when(templateService.validateBindings(templateId, groupAdmin)).thenReturn(nonBlockingBindings());
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(templateId, new PublishTemplateRequest("1.0.0"), groupAdmin))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.runtime.policyNotConfigured");
    }

    private BindingValidationView nonBlockingBindings() {
        return new BindingValidationView(
                List.of(),
                new BindingValidationSummaryView(false, 0, 0, 0, 0, 0)
        );
    }

    private BindingValidationView blockingBindings() {
        return new BindingValidationView(
                List.of(),
                new BindingValidationSummaryView(true, 1, 0, 1, 0, 0)
        );
    }
}
