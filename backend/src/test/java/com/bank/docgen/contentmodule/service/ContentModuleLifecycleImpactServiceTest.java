package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventRepository;
import com.bank.docgen.runtime.service.RuntimeGenerationAuditRecorder;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
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
class ContentModuleLifecycleImpactServiceTest {

    private static final UUID MODULE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_V1 = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEMPLATE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID TEMPLATE_VERSION_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock
    private ContentModuleVersionRepository versionRepository;
    @Mock
    private TemplateContentModuleReferenceRepository referenceRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private TemplateRepository templateRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private RuntimeGenerationAuditEventRepository runtimeAuditRepository;
    @Mock
    private com.bank.docgen.contentmodule.persistence.ContentModuleRepository moduleRepository;
    @Mock
    private com.bank.docgen.authorization.management.service.GroupAccessService groupAccessService;

    private ContentModuleLifecycleImpactService service;
    private ManagementSessionClaims groupAdmin;

    @BeforeEach
    void setUp() {
        ContentModuleAccessService accessSupport = new ContentModuleAccessService(
                moduleRepository,
                groupAccessService,
                new ObjectMapper()
        );
        service = new ContentModuleLifecycleImpactService(
                accessSupport,
                groupAccessService,
                versionRepository,
                referenceRepository,
                templateVersionRepository,
                templateRepository,
                apiPolicyRepository,
                runtimeAuditRepository
        );
        groupAdmin = new ManagementSessionClaims(
                "10000002",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                List.of("GROUP_ADMIN"),
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void preview_listsReferencingTemplatesWithRuntimeCallsAndStopFlags() {
        ContentModuleEntity module = new ContentModuleEntity(
                MODULE_ID,
                "MOD-LOAN-DISCLOSURE",
                "RETAIL",
                "Loan Disclosure",
                "desc",
                "[]",
                "10000002"
        );
        ContentModuleVersionEntity version = approvedActiveVersion();
        TemplateContentModuleReferenceEntity reference = new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(),
                TEMPLATE_VERSION_ID,
                "CLAUSE-1",
                VERSION_V1
        );
        reference.lock();
        TemplateVersionEntity templateVersion = new TemplateVersionEntity(TEMPLATE_VERSION_ID, TEMPLATE_ID, "10000002");
        templateVersion.setReleaseVersion("1.0.0");
        templateVersion.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        TemplateEntity template = new TemplateEntity(
                TEMPLATE_ID,
                "TPL-LOAN-NOTICE",
                "RETAIL",
                "Loan Notice",
                null,
                UUID.randomUUID(),
                "10000002"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);

        when(groupAccessService.canBrowseContentModuleCatalog(groupAdmin)).thenReturn(true);
        when(moduleRepository.findByModuleCodeAndDeletedAtIsNull("MOD-LOAN-DISCLOSURE"))
                .thenReturn(Optional.of(module));
        when(groupAccessService.canAccessGroup(groupAdmin, "RETAIL")).thenReturn(true);
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(MODULE_ID)).thenReturn(List.of(version));
        when(referenceRepository.findByContentModuleVersionIdIn(any())).thenReturn(List.of(reference));
        when(templateVersionRepository.findById(TEMPLATE_VERSION_ID)).thenReturn(Optional.of(templateVersion));
        when(templateRepository.findByIdAndDeletedAtIsNull(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(apiPolicy("1.0.0")));
        when(versionRepository.findByModuleIdAndReviewStateAndLifecycleStateOrderBySemanticVersionDesc(
                MODULE_ID,
                ContentModuleReviewState.APPROVED,
                ContentModuleLifecycleState.ACTIVE
        )).thenReturn(List.of(version));
        when(runtimeAuditRepository.countByTemplateIdInAndEventAtAfterAndEventTypeIn(
                any(),
                any(),
                eq(List.of(
                        RuntimeGenerationAuditRecorder.EVENT_SYNC_GENERATION,
                        RuntimeGenerationAuditRecorder.EVENT_BATCH_SYNC
                ))
        )).thenReturn(12L);

        var impact = service.previewImpact("MOD-LOAN-DISCLOSURE", groupAdmin);

        assertThat(impact.referenceTemplateCount()).isEqualTo(1);
        assertThat(impact.referenceTemplateListHint()).contains("TPL-LOAN-NOTICE");
        assertThat(impact.impactedReleaseVersionsHint()).contains("1.0.0");
        assertThat(impact.defaultRouteAffected()).isTrue();
        assertThat(impact.recentCallSummary()).isEqualTo("recentCalls=12/7d");
        assertThat(impact.templateStopRequired()).isTrue();
        assertThat(impact.releaseStopRequired()).isTrue();
    }

    @Test
    void preview_rejectsTesterCatalogBrowse() {
        ManagementSessionClaims tester = new ManagementSessionClaims(
                "10000006",
                "Tester",
                "tester@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_TESTER"),
                List.of("RETAIL"),
                "route.dashboard-home",
                List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
        when(groupAccessService.canBrowseContentModuleCatalog(tester)).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.previewImpact("MOD-LOAN-DISCLOSURE", tester)
        ).isInstanceOf(ContentModuleAccessDeniedException.class);
    }

    private ContentModuleVersionEntity approvedActiveVersion() {
        ContentModuleVersionEntity version = new ContentModuleVersionEntity(
                VERSION_V1,
                MODULE_ID,
                "1.0.0",
                "{}",
                "approved",
                "10000002"
        );
        version.setReviewState(ContentModuleReviewState.APPROVED);
        version.setLifecycleState(ContentModuleLifecycleState.ACTIVE);
        return version;
    }

    private ApiPolicyEntity apiPolicy(String defaultRoute) {
        ApiPolicyEntity policy = new ApiPolicyEntity(UUID.randomUUID(), TEMPLATE_ID, "[\"RETAIL_API\"]", "10000002");
        policy.replaceConfiguration(
                "[\"RETAIL_API\"]",
                defaultRoute,
                "[\"DOCX\"]",
                "[\"SYNC_STREAM\"]",
                false,
                10,
                false,
                false,
                "10000002"
        );
        return policy;
    }
}
