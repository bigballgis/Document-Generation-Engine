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
import com.bank.docgen.master.domain.MasterDocumentStatus;
import com.bank.docgen.master.persistence.MasterDocumentEntity;
import com.bank.docgen.master.persistence.MasterDocumentRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.document.compute.ComputeDslLimits;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.CreateTemplateRequest;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.mapping.TemplateViewMapper;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

/**
 * BDD-IBL-E1-001 / 002 / 004 — template locale declare + family uniqueness.
 */
@ExtendWith(MockitoExtension.class)
class IblE1LocaleVariantModelTest {

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
    @Mock
    private ManagementUserDisplayService managementUserDisplayService;

    private TemplateService service;
    private ManagementSessionClaims author;
    private UUID masterId;

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
                eventPublisher,
                managementUserDisplayService,
                org.mockito.Mockito.mock(VariableComputeService.class)
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
        masterId = UUID.randomUUID();
    }

    @Test
    void createTemplate_requiresLocale_bddE1001() {
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreateTemplateRequest(
                        "TPL-EN",
                        "RETAIL",
                        "EN letter",
                        null,
                        masterId.toString(),
                        "   ",
                        null
                ),
                author
        )).isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.template.localeRequired");
    }

    @Test
    void createTemplate_persistsLocaleAndFamily_bddE1002() {
        stubApprovedMaster();
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        when(templateRepository.findByExternalIdAndDeletedAtIsNull("TPL-EN")).thenReturn(Optional.empty());
        when(templateRepository.existsByGroupCodeAndLocaleVariantFamilyIdAndLocaleAndDeletedAtIsNull(
                any(), any(), any())).thenReturn(false);
        UUID familyId = UUID.randomUUID();
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), UUID.randomUUID(), "10000003");
        when(templateCurrentVersionResolver.findInFlightDevVersion(any())).thenReturn(Optional.of(version));
        when(templateCurrentVersionResolver.isInFlight(any())).thenReturn(true);
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(any())).thenReturn(List.of());
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(any())).thenReturn(List.of());
        when(templateRepository.save(any(TemplateEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(templateVersionRepository.save(any(TemplateVersionEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        TemplateDetailView detail = service.create(
                new CreateTemplateRequest(
                        "TPL-EN",
                        "RETAIL",
                        "EN letter",
                        "desc",
                        masterId.toString(),
                        "en-US",
                        familyId
                ),
                author
        );

        ArgumentCaptor<TemplateEntity> captor = ArgumentCaptor.forClass(TemplateEntity.class);
        verify(templateRepository).save(captor.capture());
        assertThat(captor.getValue().getLocale()).isEqualTo("en-US");
        assertThat(captor.getValue().getLocaleVariantFamilyId()).isEqualTo(familyId);
        assertThat(detail.locale()).isEqualTo("en-US");
        assertThat(detail.localeVariantFamilyId()).isEqualTo(familyId.toString());
    }

    @Test
    void createTemplate_familyLocaleConflict_bddE1004() {
        when(groupAccessService.canAuthorTemplates(author)).thenReturn(true);
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);
        UUID familyId = UUID.randomUUID();
        when(templateRepository.existsByGroupCodeAndLocaleVariantFamilyIdAndLocaleAndDeletedAtIsNull(
                eq("RETAIL"), eq(familyId), eq("en-US"))).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                new CreateTemplateRequest(
                        "TPL-EN-2",
                        "RETAIL",
                        "EN letter 2",
                        null,
                        masterId.toString(),
                        "en-US",
                        familyId
                ),
                author
        )).isInstanceOf(TemplateGovernanceException.class)
                .satisfies(ex -> {
                    TemplateGovernanceException gov = (TemplateGovernanceException) ex;
                    assertThat(gov.errorCode()).isEqualTo(ApiErrorCodes.LOCALE_VARIANT_CONFLICT);
                    assertThat(gov.httpStatus()).isEqualTo(HttpStatus.CONFLICT);
                });
    }

    @Test
    void entityDefaultsLocaleToZhCnForLegacyConstruction_bddE1012() {
        TemplateEntity template = new TemplateEntity(
                UUID.randomUUID(),
                "TPL-LEGACY",
                "RETAIL",
                "Legacy",
                null,
                masterId,
                "10000003"
        );
        assertThat(template.getLocale()).isEqualTo(ComputeDslLimits.DEFAULT_LOCALE);
        assertThat(template.getLifecycleStatus()).isEqualTo(TemplateLifecycleStatus.DRAFT);
    }

    private void stubApprovedMaster() {
        MasterDocumentEntity master = org.mockito.Mockito.mock(MasterDocumentEntity.class);
        when(master.getStatus()).thenReturn(MasterDocumentStatus.APPROVED);
        when(master.getGroupCode()).thenReturn("RETAIL");
        when(masterDocumentRepository.findByIdAndDeletedAtIsNull(masterId)).thenReturn(Optional.of(master));
    }
}
