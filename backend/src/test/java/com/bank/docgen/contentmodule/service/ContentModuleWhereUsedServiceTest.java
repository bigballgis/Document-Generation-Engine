package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleWhereUsedTemplateView;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentModuleWhereUsedServiceTest {

    private static final UUID MODULE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID VERSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TEMPLATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TEMPLATE_VERSION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID MASTER_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock private ContentModuleAccessService accessSupport;
    @Mock private GroupAccessService groupAccessService;
    @Mock private ContentModuleVersionRepository versionRepository;
    @Mock private TemplateContentModuleReferenceRepository referenceRepository;
    @Mock private TemplateVersionRepository templateVersionRepository;
    @Mock private TemplateRepository templateRepository;

    private ContentModuleWhereUsedService service;
    private ManagementSessionClaims author;
    private ManagementSessionClaims tester;
    private ContentModuleEntity module;

    @BeforeEach
    void setUp() {
        ContentModuleNestingService nestingService = org.mockito.Mockito.mock(ContentModuleNestingService.class);
        org.mockito.Mockito.lenient().when(nestingService.findNestingAncestors(MODULE_ID)).thenReturn(List.of());
        service = new ContentModuleWhereUsedService(
                accessSupport,
                groupAccessService,
                versionRepository,
                referenceRepository,
                templateVersionRepository,
                templateRepository,
                nestingService
        );
        author = new ManagementSessionClaims(
                "10000003", "Author", "a@example.com", AuthSource.LOCAL,
                List.of("DOCUMENT_AUTHOR"), List.of("RETAIL"),
                "route.home", List.of("route.home"), Instant.now().plusSeconds(3600)
        );
        tester = new ManagementSessionClaims(
                "10000006", "Tester", "t@example.com", AuthSource.LOCAL,
                List.of("TEMPLATE_TESTER"), List.of("RETAIL"),
                "route.home", List.of("route.home"), Instant.now().plusSeconds(3600)
        );
        module = new ContentModuleEntity(
                MODULE_ID, "MOD-X", "RETAIL", "Module X", "d", "[]", "10000003"
        );
    }

    @Test
    void listWhereUsed_returnsAuthorizedReferencingTemplates() {
        when(groupAccessService.canBrowseContentModuleCatalog(author)).thenReturn(true);
        when(accessSupport.requireReadableModule(MODULE_ID.toString(), author)).thenReturn(module);
        ContentModuleVersionEntity cmVersion = new ContentModuleVersionEntity(
                VERSION_ID, MODULE_ID, "1.0.0", "{\"blocks\":[]}", "init", "10000003"
        );
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(MODULE_ID))
                .thenReturn(List.of(cmVersion));
        TemplateContentModuleReferenceEntity ref = new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(), TEMPLATE_VERSION_ID, "clause-1", VERSION_ID
        );
        when(referenceRepository.findByContentModuleVersionIdIn(Set.of(VERSION_ID)))
                .thenReturn(List.of(ref));
        TemplateVersionEntity tv = new TemplateVersionEntity(TEMPLATE_VERSION_ID, TEMPLATE_ID, "10000003");
        when(templateVersionRepository.findById(TEMPLATE_VERSION_ID)).thenReturn(Optional.of(tv));
        TemplateEntity template = new TemplateEntity(
                TEMPLATE_ID, "TPL-1", "RETAIL", "Template One", null, MASTER_ID, "10000003"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        when(templateRepository.findByIdAndDeletedAtIsNull(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);

        PageView<ContentModuleWhereUsedTemplateView> page =
                service.listWhereUsed(MODULE_ID.toString(), 0, 20, author);

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().getFirst().externalId()).isEqualTo("TPL-1");
        assertThat(page.content().getFirst().pinnedSemanticVersion()).isEqualTo("1.0.0");
        assertThat(page.content().getFirst().referenceKind())
                .isEqualTo(com.bank.docgen.contentmodule.domain.ContentModuleWhereUsedReferenceKind.DIRECT);
        assertThat(page.content().getFirst().nestingDepth()).isZero();
    }

    @Test
    void listWhereUsed_emptyWhenNoReferences() {
        when(groupAccessService.canBrowseContentModuleCatalog(author)).thenReturn(true);
        when(accessSupport.requireReadableModule(MODULE_ID.toString(), author)).thenReturn(module);
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(MODULE_ID))
                .thenReturn(List.of(new ContentModuleVersionEntity(
                        VERSION_ID, MODULE_ID, "1.0.0", "{}", "i", "10000003"
                )));
        when(referenceRepository.findByContentModuleVersionIdIn(Set.of(VERSION_ID)))
                .thenReturn(List.of());

        PageView<ContentModuleWhereUsedTemplateView> page =
                service.listWhereUsed(MODULE_ID.toString(), 0, 20, author);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void listWhereUsed_testerDenied() {
        when(groupAccessService.canBrowseContentModuleCatalog(tester)).thenReturn(false);
        assertThatThrownBy(() -> service.listWhereUsed(MODULE_ID.toString(), 0, 20, tester))
                .isInstanceOf(ContentModuleAccessDeniedException.class);
    }
}
