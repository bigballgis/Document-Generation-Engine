package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.CatalogQueryPage;
import com.bank.docgen.authorization.management.api.CatalogSortKey;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.contentmodule.domain.ContentModuleSearchMode;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepositoryCustom.ContentModuleCatalogFilter;
import com.bank.docgen.contentmodule.persistence.ContentModuleReviewRecordRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContentModuleFullTextSearchModeTest {

    private static final UUID MODULE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock private ContentModuleRepository moduleRepository;
    @Mock private ContentModuleVersionRepository versionRepository;
    @Mock private ContentModuleReviewRecordRepository reviewRecordRepository;
    @Mock private GroupAccessService groupAccessService;
    @Mock private ManagementAuditRecorder auditRecorder;
    @Mock private ContentModuleFullTextIndexWriter fullTextIndexWriter;

    private ContentModuleService service;
    private ManagementSessionClaims author;
    private ContentModuleEntity module;

    @BeforeEach
    void setUp() {
        ContentModuleAccessService accessSupport =
                new ContentModuleAccessService(moduleRepository, groupAccessService, new ObjectMapper());
        service = new ContentModuleService(
                moduleRepository,
                versionRepository,
                reviewRecordRepository,
                groupAccessService,
                accessSupport,
                auditRecorder,
                fullTextIndexWriter
        );
        module = new ContentModuleEntity(
                MODULE_ID, "MOD-FTS", "RETAIL", "FTS Module", "d", "[]", "10000003"
        );
        author = new ManagementSessionClaims(
                "10000003", "Author", "a@example.com", AuthSource.LOCAL,
                List.of("TEMPLATE_AUTHOR"), List.of("RETAIL"),
                "route.home", List.of("route.home"), Instant.now().plusSeconds(3600)
        );
        when(groupAccessService.canBrowseContentModuleCatalog(author)).thenReturn(true);
        when(groupAccessService.accessibleGroupCodes(author)).thenReturn(List.of("RETAIL"));
        when(versionRepository.findByModuleIdIn(any())).thenReturn(List.of(
                new ContentModuleVersionEntity(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        MODULE_ID, "1.0.0", "{}", "i", "10000003"
                )
        ));
        when(moduleRepository.searchCatalog(any(), eq(0), eq(20)))
                .thenReturn(new CatalogQueryPage<>(List.of(module), 1, 1));
    }

    @Test
    void nameMode_defaultKeepsIlikePath() {
        service.list(author, 0, 20, "Alpha", null, null, null, null, null, null, null, null);

        ArgumentCaptor<ContentModuleCatalogFilter> captor = ArgumentCaptor.forClass(ContentModuleCatalogFilter.class);
        verify(moduleRepository).searchCatalog(captor.capture(), eq(0), eq(20));
        assertThat(captor.getValue().searchMode()).isEqualTo(ContentModuleSearchMode.NAME);
        assertThat(captor.getValue().search()).isEqualTo("Alpha");
        assertThat(captor.getValue().isFullTextSearch()).isFalse();
        assertThat(captor.getValue().sort()).isEqualTo(CatalogSortKey.GROUP_CODE_ASC);
    }

    @Test
    void fullTextMode_setsFlagAndDefaultRankSortNull() {
        service.list(
                author, 0, 20, "force majeure carve-out-xyz", null, null,
                null, null, null, null, null, "FULL_TEXT"
        );

        ArgumentCaptor<ContentModuleCatalogFilter> captor = ArgumentCaptor.forClass(ContentModuleCatalogFilter.class);
        verify(moduleRepository).searchCatalog(captor.capture(), eq(0), eq(20));
        assertThat(captor.getValue().searchMode()).isEqualTo(ContentModuleSearchMode.FULL_TEXT);
        assertThat(captor.getValue().isFullTextSearch()).isTrue();
        assertThat(captor.getValue().sort()).isNull();
    }

    @Test
    void invalidSearchMode_throwsValidation() {
        assertThatThrownBy(() -> service.list(
                author, 0, 20, "x", null, null, null, null, null, null, null, "BOGUS"
        )).isInstanceOf(ContentModuleValidationException.class)
                .hasMessageContaining("searchModeInvalid");
    }

    @Test
    void searchTooLong_throwsValidation() {
        String longSearch = "a".repeat(201);
        assertThatThrownBy(() -> service.list(
                author, 0, 20, longSearch, null, null, null, null, null, null, null, "NAME"
        )).isInstanceOf(ContentModuleValidationException.class)
                .hasMessageContaining("searchTooLong");
    }
}
