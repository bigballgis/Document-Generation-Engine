package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.UpdateTemplateRequest;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.service.TemplateCurrentVersionResolver;
import org.springframework.context.ApplicationEventPublisher;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class TemplateServiceMetadataTest {

    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private VariableSchemaRepository variableSchemaRepository;
    @Mock
    private AnchorBindingRepository anchorBindingRepository;
    @Mock
    private MasterDocumentRepository masterDocumentRepository;
    @Mock
    private TemplateLifecycleRecordRepository lifecycleRecordRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;

    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private TemplateStructuredAuthoringService structuredAuthoringService;
    @Mock
    private TemplateBindingConfigurationService bindingConfigurationService;
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private TemplateService service;
    private ManagementSessionClaims author;
    private UUID templateId;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        TemplateViewMapper viewMapper = new TemplateViewMapper(
                templateVersionRepository,
                variableSchemaRepository,
                anchorBindingRepository,
                new ApprovalSubStateResolver(lifecycleRecordRepository),
                objectMapper,
                templateCurrentVersionResolver
        );
        service = new TemplateService(
                templateRepository,
                templateVersionRepository,
                masterDocumentRepository,
                apiPolicyRepository,
                groupAccessService,
                structuredAuthoringService,
                bindingConfigurationService,
                viewMapper,
                templateCurrentVersionResolver,
                eventPublisher
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
                "Old name",
                "Old description",
                UUID.randomUUID(),
                "10000003"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
    }

    @Test
    void updateMetadata_inDraft_updatesNameAndDescription() {
        when(templateRepository.findByIdAndDeletedAtIsNull(templateId)).thenReturn(Optional.of(template));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), templateId, "10000003");
        when(templateCurrentVersionResolver.findInFlightDevVersion(templateId))
                .thenReturn(Optional.of(version));
        when(templateCurrentVersionResolver.isInFlight(any())).thenReturn(true);
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(version.getId()))
                .thenReturn(List.of());
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(version.getId()))
                .thenReturn(List.of());

        TemplateDetailView result = service.updateMetadata(
                templateId,
                new UpdateTemplateRequest("New name", "New description"),
                author
        );

        assertThat(result.name()).isEqualTo("New name");
        assertThat(result.description()).isEqualTo("New description");
        verify(templateRepository).save(template);
    }

    @Test
    void updateMetadata_onPublishedTemplate_throwsInvalidState() {
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        when(templateRepository.findByIdAndDeletedAtIsNull(templateId)).thenReturn(Optional.of(template));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);

        assertThatThrownBy(() -> service.updateMetadata(
                templateId,
                new UpdateTemplateRequest("New name", null),
                author
        )).isInstanceOf(TemplateValidationException.class);
    }
}
