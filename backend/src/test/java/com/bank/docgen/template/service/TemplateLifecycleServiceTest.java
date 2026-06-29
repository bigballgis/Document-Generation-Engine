package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.collaboration.service.CollaborationWorkItemWriter;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleCommentRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateLifecycleServiceTest {

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
    private PublishGateService publishGateService;
    @Mock
    private DecisionFormService decisionFormService;
    @Mock
    private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock
    private CollaborationWorkItemWriter collaborationWorkItemWriter;
    @Mock
    private com.bank.docgen.authoring.structured.RenderProfileService renderProfileService;

    private TemplateLifecycleService service;
    private ManagementSessionClaims author;
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
                publishGateService,
                decisionFormService,
                contentModuleReferenceService,
                collaborationWorkItemWriter,
                renderProfileService
        );
        author = new ManagementSessionClaims(
                "10000003",
                "Author",
                "author@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
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
                "10000003"
        );
    }

    @Test
    void submitForTest_transitionsToTestingAndCreatesCollaborationWorkItem() {
        when(templateService.requireWritableTemplate(templateId, author)).thenReturn(template);
        when(templateService.toDetail(template)).thenReturn(detail(TemplateLifecycleStatus.TESTING));

        TemplateDetailView result = service.submitForTest(
                templateId,
                new LifecycleCommentRequest("Ready for test"),
                author
        );

        assertThat(result.lifecycleStatus()).isEqualTo(TemplateLifecycleStatus.TESTING);
        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.TESTING);
        verify(collaborationWorkItemWriter).upsertSubmitForTestWorkItem(eq(template), eq(author));
    }

    private TemplateDetailView detail(TemplateLifecycleStatus status) {
        return new TemplateDetailView(
                templateId.toString(),
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID().toString(),
                status,
                null,
                null,
                UUID.randomUUID().toString(),
                1,
                List.of(),
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now()
        );
    }
}
