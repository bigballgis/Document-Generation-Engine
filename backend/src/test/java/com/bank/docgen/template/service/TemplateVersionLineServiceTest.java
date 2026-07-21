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
import com.bank.docgen.authorization.management.service.ManagementUserDisplayService;
import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateDevVersionCreatedView;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.TemplateExportMasterPinView;
import com.bank.docgen.template.api.TemplateVersionLineDetailView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.domain.TemplateVersionLineKind;
import com.bank.docgen.template.mapping.TemplateMasterPinMapper;
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
    @Mock
    private ManagementUserDisplayService managementUserDisplayService;
    @Mock
    private TemplateMasterPinMapper templateMasterPinMapper;

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
                messageResolver,
                managementUserDisplayService,
                templateMasterPinMapper
        );
        author = new ManagementSessionClaims(
                "10000003",
                "Author",
                "author@example.com",
                AuthSource.LOCAL,
                List.of("DOCUMENT_AUTHOR"),
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
    void get_includesMasterPinWhenPublishedLineIsPinned() {
        UUID versionId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        String hash = "c".repeat(64);
        TemplateVersionEntity published = publishedVersion(versionId, 1, "1.0.0");
        published.setMasterRevisionId(revisionId);
        published.setMasterFileHash(hash);
        published.setPinMetadataJson("{\"pinOrigin\":\"PUBLISHED\"}");
        TemplateExportMasterPinView pin = new TemplateExportMasterPinView(
                revisionId.toString(), hash, 2, "PUBLISHED"
        );

        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(templateVersionRepository.findById(versionId)).thenReturn(Optional.of(published));
        when(templateCurrentVersionResolver.isInFlight(published)).thenReturn(false);
        when(templateCurrentVersionResolver.hasInFlightDevVersion(templateId)).thenReturn(false);
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId))
                .thenReturn(List.of());
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId))
                .thenReturn(List.of());
        when(templateViewMapper.loadRules(published)).thenReturn(List.of());
        when(templateMasterPinMapper.toView(published)).thenReturn(pin);

        TemplateVersionLineDetailView detail = service.get(templateId, versionId, author);

        assertThat(detail.masterPin()).isEqualTo(pin);
        assertThat(detail.masterPin().masterRevisionId()).isEqualTo(revisionId.toString());
        assertThat(detail.masterPin().masterFileHash()).isEqualTo(hash);
    }

    @Test
    void get_omitsMasterPinWhenInFlightUnpinned() {
        UUID versionId = UUID.randomUUID();
        TemplateVersionEntity inFlight = inFlightVersion(versionId, 2);

        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(templateVersionRepository.findById(versionId)).thenReturn(Optional.of(inFlight));
        when(templateCurrentVersionResolver.isInFlight(inFlight)).thenReturn(true);
        when(templateCurrentVersionResolver.hasInFlightDevVersion(templateId)).thenReturn(true);
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());
        when(approvalSubStateResolver.resolve(template)).thenReturn(null);
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(versionId))
                .thenReturn(List.of());
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId))
                .thenReturn(List.of());
        when(templateViewMapper.loadRules(inFlight)).thenReturn(List.of());
        when(templateMasterPinMapper.toView(inFlight)).thenReturn(null);

        TemplateVersionLineDetailView detail = service.get(templateId, versionId, author);

        assertThat(detail.masterPin()).isNull();
    }

    @Test
    void getReleaseDetail_includesMasterPinWhenPinned() {
        UUID versionId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        String hash = "d".repeat(64);
        TemplateVersionEntity published = publishedVersion(versionId, 1, "1.0.0");
        published.setMasterRevisionId(revisionId);
        published.setMasterFileHash(hash);
        TemplateExportMasterPinView pin = new TemplateExportMasterPinView(
                revisionId.toString(), hash, 1, "PUBLISHED"
        );
        TemplateDetailView baseDetail = new TemplateDetailView(
                templateId.toString(),
                "TPL-001",
                "RETAIL",
                "Letter",
                "Desc",
                template.getMasterId().toString(),
                TemplateLifecycleStatus.PUBLISHED,
                null,
                "1.0.0",
                versionId.toString(),
                1,
                List.of(),
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now(),
                "10000003",
                null,
                true,
                null,
                null);

        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "1.0.0"))
                .thenReturn(Optional.of(published));
        when(templateViewMapper.toDetailForVersion(template, published, true)).thenReturn(baseDetail);
        when(templateMasterPinMapper.toView(published)).thenReturn(pin);
        when(managementUserDisplayService.lookupDisplayNames(any())).thenReturn(Map.of("10000003", "Author"));

        TemplateDetailView detail = service.getReleaseDetail(templateId, "1.0.0", author);

        assertThat(detail.masterPin()).isEqualTo(pin);
        assertThat(detail.readOnly()).isTrue();
    }

    @Test
    void getReleaseDetail_omitsMasterPinWhenUnpinned() {
        UUID versionId = UUID.randomUUID();
        TemplateVersionEntity published = publishedVersion(versionId, 1, "1.0.0");
        TemplateDetailView baseDetail = new TemplateDetailView(
                templateId.toString(),
                "TPL-001",
                "RETAIL",
                "Letter",
                "Desc",
                template.getMasterId().toString(),
                TemplateLifecycleStatus.PUBLISHED,
                null,
                "1.0.0",
                versionId.toString(),
                1,
                List.of(),
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now(),
                "10000003",
                null,
                true,
                null,
                null);

        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "1.0.0"))
                .thenReturn(Optional.of(published));
        when(templateViewMapper.toDetailForVersion(template, published, true)).thenReturn(baseDetail);
        when(templateMasterPinMapper.toView(published)).thenReturn(null);
        when(managementUserDisplayService.lookupDisplayNames(any())).thenReturn(Map.of());

        TemplateDetailView detail = service.getReleaseDetail(templateId, "1.0.0", author);

        assertThat(detail.masterPin()).isNull();
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
                        "Customer",
                        null
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
