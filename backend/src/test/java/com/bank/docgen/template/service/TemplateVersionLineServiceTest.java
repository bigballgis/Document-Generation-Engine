package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateDevVersionCreatedView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.domain.TemplateVersionLineKind;
import com.bank.docgen.template.mapping.TemplateViewMapper;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.domain.VariableType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TemplateVersionLineServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;
    @Mock
    private VariableSchemaRepository variableSchemaRepository;
    @Mock
    private AnchorBindingRepository anchorBindingRepository;
    @Mock
    private TemplateContentModuleReferenceRepository contentModuleReferenceRepository;
    @Mock
    private TemplateLifecycleRecordRepository lifecycleRecordRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private TemplateViewMapper templateViewMapper;
    @Mock
    private ApprovalSubStateResolver approvalSubStateResolver;
    @Mock
    private GroupAccessService groupAccessService;
    @Mock
    private MessageResolver messageResolver;

    private TemplateVersionLineService service;
    private ManagementSessionClaims author;
    private UUID templateId;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        service = new TemplateVersionLineService(
                templateService,
                templateRepository,
                templateVersionRepository,
                templateCurrentVersionResolver,
                variableSchemaRepository,
                anchorBindingRepository,
                contentModuleReferenceRepository,
                lifecycleRecordRepository,
                apiPolicyRepository,
                templateViewMapper,
                approvalSubStateResolver,
                groupAccessService,
                messageResolver
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
                "Letter",
                "Desc",
                UUID.randomUUID(),
                "10000003"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
    }

    @Test
    void list_returnsInFlightAndPublishedRows() {
        TemplateVersionEntity published = publishedVersion(UUID.randomUUID(), 1, "1.0.0");
        TemplateVersionEntity inFlight = inFlightVersion(UUID.randomUUID(), 2);
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(templateCurrentVersionResolver.listVersionLinesOrdered(templateId))
                .thenReturn(List.of(inFlight, published));
        when(templateCurrentVersionResolver.isInFlight(inFlight)).thenReturn(true);
        when(templateCurrentVersionResolver.isInFlight(published)).thenReturn(false);
        when(templateCurrentVersionResolver.hasInFlightDevVersion(templateId)).thenReturn(true);
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());
        when(approvalSubStateResolver.resolve(template)).thenReturn(null);

        var page = service.list(templateId, 0, 20, author);

        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0).lineKind()).isEqualTo(TemplateVersionLineKind.IN_FLIGHT);
        assertThat(page.content().get(1).lineKind()).isEqualTo(TemplateVersionLineKind.PUBLISHED);
        assertThat(page.content().get(1).releaseVersion()).isEqualTo("1.0.0");
    }

    @Test
    void cloneReleaseVersion_copiesBindingsAndBlocksWhenInFlightExists() {
        UUID sourceId = UUID.randomUUID();
        TemplateVersionEntity source = publishedVersion(sourceId, 1, "1.0.0");
        when(templateService.requireWritableTemplate(templateId, author)).thenReturn(template);
        when(templateCurrentVersionResolver.hasInFlightDevVersion(templateId)).thenReturn(true);

        assertThatThrownBy(() -> service.cloneReleaseVersion(templateId, "1.0.0", author))
                .isInstanceOf(TemplateGovernanceException.class);
    }

    @Test
    void cloneReleaseVersion_createsNewDraftDevLineAndCopiesChildEntities() {
        UUID sourceId = UUID.randomUUID();
        TemplateVersionEntity source = publishedVersion(sourceId, 1, "1.0.0");
        when(templateService.requireWritableTemplate(templateId, author)).thenReturn(template);
        when(templateCurrentVersionResolver.hasInFlightDevVersion(templateId)).thenReturn(false);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "1.0.0"))
                .thenReturn(Optional.of(source));
        when(templateCurrentVersionResolver.maxDevVersionNumber(templateId)).thenReturn(1);
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(sourceId))
                .thenReturn(List.of(new VariableSchemaEntity(
                        UUID.randomUUID(),
                        sourceId,
                        "customerName",
                        VariableType.TEXT,
                        true,
                        "Acme",
                        null,
                        "Customer"
                )));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(sourceId))
                .thenReturn(List.of(new AnchorBindingEntity(
                        UUID.randomUUID(),
                        sourceId,
                        "HEADER",
                        AnchorContentType.TEXT,
                        "{}",
                        BindingValidationStatus.VALID
                )));
        when(contentModuleReferenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(sourceId))
                .thenReturn(List.of());
        when(messageResolver.resolve(any(), any(), any(), any())).thenReturn("cloned");

        TemplateDevVersionCreatedView created = service.cloneReleaseVersion(templateId, "1.0.0", author);

        assertThat(created.devVersionNumber()).isEqualTo(2);
        ArgumentCaptor<TemplateVersionEntity> savedVersion = ArgumentCaptor.forClass(TemplateVersionEntity.class);
        verify(templateVersionRepository).save(savedVersion.capture());
        assertThat(savedVersion.getValue().getDevVersionNumber()).isEqualTo(2);
        verify(variableSchemaRepository).save(any(VariableSchemaEntity.class));
        verify(anchorBindingRepository).save(any(AnchorBindingEntity.class));
        verify(templateRepository).save(template);
        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.DRAFT);
    }

    private TemplateVersionEntity publishedVersion(UUID id, int devVersionNumber, String releaseVersion) {
        TemplateVersionEntity entity = new TemplateVersionEntity(id, templateId, "10000003");
        entity.setDevVersionNumber(devVersionNumber);
        entity.setReleaseVersion(releaseVersion);
        entity.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        return entity;
    }

    private TemplateVersionEntity inFlightVersion(UUID id, int devVersionNumber) {
        TemplateVersionEntity entity = new TemplateVersionEntity(id, templateId, "10000003");
        entity.setDevVersionNumber(devVersionNumber);
        entity.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        return entity;
    }
}
