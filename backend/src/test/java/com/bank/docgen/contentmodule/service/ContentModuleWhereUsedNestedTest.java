package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.api.ContentModuleNestingAncestorHit;
import com.bank.docgen.contentmodule.api.ContentModuleWhereUsedTemplateView;
import com.bank.docgen.contentmodule.domain.ContentModuleWhereUsedReferenceKind;
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
class ContentModuleWhereUsedNestedTest {

    private static final UUID CHILD_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CHILD_VERSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PARENT_VERSION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PARENT_MODULE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID TEMPLATE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TEMPLATE_VERSION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock private ContentModuleAccessService accessSupport;
    @Mock private GroupAccessService groupAccessService;
    @Mock private ContentModuleVersionRepository versionRepository;
    @Mock private TemplateContentModuleReferenceRepository referenceRepository;
    @Mock private TemplateVersionRepository templateVersionRepository;
    @Mock private TemplateRepository templateRepository;
    @Mock private ContentModuleNestingService nestingService;

    private ContentModuleWhereUsedService service;
    private ManagementSessionClaims author;

    @BeforeEach
    void setUp() {
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
    }

    @Test
    void listWhereUsed_reportsNestedTemplateViaAncestorPin() {
        ContentModuleEntity child = new ContentModuleEntity(
                CHILD_ID, "MOD-CHILD", "RETAIL", "Child", "d", "[]", "10000003");
        when(groupAccessService.canBrowseContentModuleCatalog(author)).thenReturn(true);
        when(accessSupport.requireReadableModule(CHILD_ID.toString(), author)).thenReturn(child);
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(CHILD_ID))
                .thenReturn(List.of(new ContentModuleVersionEntity(
                        CHILD_VERSION_ID, CHILD_ID, "1.0.0", "{}", "i", "10000003")));
        when(referenceRepository.findByContentModuleVersionIdIn(Set.of(CHILD_VERSION_ID)))
                .thenReturn(List.of());
        when(nestingService.findNestingAncestors(CHILD_ID)).thenReturn(List.of(
                new ContentModuleNestingAncestorHit(
                        PARENT_VERSION_ID, PARENT_MODULE_ID, "MOD-PARENT", 1, "MOD-PARENT>MOD-CHILD")));
        TemplateContentModuleReferenceEntity parentPin = new TemplateContentModuleReferenceEntity(
                UUID.randomUUID(), TEMPLATE_VERSION_ID, "PARENT", PARENT_VERSION_ID);
        when(referenceRepository.findByContentModuleVersionIdIn(List.of(PARENT_VERSION_ID)))
                .thenReturn(List.of(parentPin));
        TemplateVersionEntity tv = new TemplateVersionEntity(TEMPLATE_VERSION_ID, TEMPLATE_ID, "10000003");
        when(templateVersionRepository.findById(TEMPLATE_VERSION_ID)).thenReturn(Optional.of(tv));
        TemplateEntity template = new TemplateEntity(
                TEMPLATE_ID, "TPL-NEST", "RETAIL", "Nested Template", null, UUID.randomUUID(), "10000003");
        template.setLifecycleStatus(TemplateLifecycleStatus.DRAFT);
        when(templateRepository.findByIdAndDeletedAtIsNull(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(groupAccessService.canAccessGroup(author, "RETAIL")).thenReturn(true);

        PageView<ContentModuleWhereUsedTemplateView> page =
                service.listWhereUsed(CHILD_ID.toString(), 0, 20, author);

        assertThat(page.content()).hasSize(1);
        ContentModuleWhereUsedTemplateView row = page.content().getFirst();
        assertThat(row.referenceKind()).isEqualTo(ContentModuleWhereUsedReferenceKind.NESTED);
        assertThat(row.nestingDepth()).isEqualTo(1);
        assertThat(row.nestingPathSummary()).isEqualTo("MOD-PARENT>MOD-CHILD");
        assertThat(row.externalId()).isEqualTo("TPL-NEST");
    }
}
