package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleLifecycleImpactSummaryView;
import com.bank.docgen.contentmodule.api.ContentModuleNestingAncestorHit;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.runtime.persistence.RuntimeGenerationAuditEventRepository;
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
class ContentModuleLifecycleImpactNestedTest {

    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CHILD_VERSION = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PARENT_VERSION = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PARENT_MODULE = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID TEMPLATE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID TEMPLATE_VERSION = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    @Mock private ContentModuleVersionRepository versionRepository;
    @Mock private TemplateContentModuleReferenceRepository referenceRepository;
    @Mock private TemplateVersionRepository templateVersionRepository;
    @Mock private TemplateRepository templateRepository;
    @Mock private ApiPolicyRepository apiPolicyRepository;
    @Mock private RuntimeGenerationAuditEventRepository runtimeAuditRepository;
    @Mock private ContentModuleRepository moduleRepository;
    @Mock private GroupAccessService groupAccessService;
    @Mock private ContentModuleNestingService nestingService;

    private ContentModuleLifecycleImpactService service;
    private ManagementSessionClaims admin;

    @BeforeEach
    void setUp() {
        ContentModuleAccessService accessSupport = new ContentModuleAccessService(
                moduleRepository, groupAccessService, new ObjectMapper());
        service = new ContentModuleLifecycleImpactService(
                accessSupport,
                groupAccessService,
                versionRepository,
                referenceRepository,
                templateVersionRepository,
                templateRepository,
                apiPolicyRepository,
                runtimeAuditRepository,
                nestingService
        );
        admin = new ManagementSessionClaims(
                "10000002", "Admin", "admin@example.com", AuthSource.LOCAL,
                List.of("GROUP_ADMIN"), List.of("RETAIL"),
                "route.dashboard-home", List.of("route.dashboard-home"),
                Instant.now().plusSeconds(3600)
        );
    }

    @Test
    void previewImpact_includesTemplatePinnedOnlyToNestingAncestor() {
        ContentModuleEntity child = new ContentModuleEntity(
                CHILD_ID, "MOD-CHILD", "RETAIL", "Child", "d", "[]", "10000002");
        when(groupAccessService.canBrowseContentModuleCatalog(admin)).thenReturn(true);
        when(moduleRepository.findByIdAndDeletedAtIsNull(CHILD_ID)).thenReturn(Optional.of(child));
        when(groupAccessService.canAccessGroup(admin, "RETAIL")).thenReturn(true);
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(CHILD_ID))
                .thenReturn(List.of(new ContentModuleVersionEntity(
                        CHILD_VERSION, CHILD_ID, "1.0.0", "{}", "i", "10000002")));
        when(nestingService.findNestingAncestors(CHILD_ID)).thenReturn(List.of(
                new ContentModuleNestingAncestorHit(
                        PARENT_VERSION, PARENT_MODULE, "MOD-PARENT", 1, "MOD-PARENT>MOD-CHILD")));
        when(referenceRepository.findByContentModuleVersionIdIn(any()))
                .thenReturn(List.of(new TemplateContentModuleReferenceEntity(
                        UUID.randomUUID(), TEMPLATE_VERSION, "PARENT", PARENT_VERSION)));
        TemplateVersionEntity tv = new TemplateVersionEntity(TEMPLATE_VERSION, TEMPLATE_ID, "10000002");
        when(templateVersionRepository.findById(TEMPLATE_VERSION)).thenReturn(Optional.of(tv));
        TemplateEntity template = new TemplateEntity(
                TEMPLATE_ID, "TPL-NEST", "RETAIL", "Nested", null, UUID.randomUUID(), "10000002");
        template.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        when(templateRepository.findByIdAndDeletedAtIsNull(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(versionRepository.findByModuleIdAndReviewStateAndLifecycleStateOrderBySemanticVersionDesc(
                eq(CHILD_ID), any(), any())).thenReturn(List.of());

        ContentModuleLifecycleImpactSummaryView impact =
                service.previewImpact(CHILD_ID.toString(), admin);

        assertThat(impact.referenceTemplateCount()).isEqualTo(1);
        assertThat(impact.referenceTemplateListHint()).contains("TPL-NEST");
    }
}
