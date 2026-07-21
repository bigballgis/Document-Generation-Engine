package com.bank.docgen.contentmodule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.contentmodule.api.ContentModuleNestingPublishSummaryView;
import com.bank.docgen.contentmodule.persistence.ContentModuleEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleNestingEdgeEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleNestingEdgeRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleRepository;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ContentModuleNestingServiceTest {

    private static final UUID PARENT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CHILD_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID PARENT_VERSION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CHILD_VERSION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock private ContentModuleNestingEdgeRepository edgeRepository;
    @Mock private ContentModuleRepository moduleRepository;
    @Mock private ContentModuleVersionRepository versionRepository;
    @Mock private ContentModuleAccessService accessSupport;

    private ContentModuleNestingService service;
    private ManagementSessionClaims author;
    private ContentModuleEntity child;

    @BeforeEach
    void setUp() {
        service = new ContentModuleNestingService(
                edgeRepository,
                moduleRepository,
                versionRepository,
                accessSupport,
                new ObjectMapper()
        );
        author = new ManagementSessionClaims(
                "10000003", "Author", "a@example.com", AuthSource.LOCAL,
                List.of("DOCUMENT_AUTHOR"), List.of("RETAIL"),
                "route.home", List.of("route.home"), Instant.now().plusSeconds(3600)
        );
        child = new ContentModuleEntity(CHILD_ID, "MOD-CHILD", "RETAIL", "Child", "d", "[]", "10000003");
    }

    @Test
    void validateAndSyncEdges_depthOneNest_savesEdge() {
        when(accessSupport.resolveModule("MOD-CHILD")).thenReturn(Optional.of(child));
        when(accessSupport.canAccessModule(author, child)).thenReturn(true);
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(CHILD_ID))
                .thenReturn(List.of(new ContentModuleVersionEntity(
                        CHILD_VERSION_ID, CHILD_ID, "1.0.0", "{\"nodes\":[]}", "i", "10000003")));
        when(edgeRepository.findByParentVersionId(CHILD_VERSION_ID)).thenReturn(List.of());

        String structure = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"MOD-CHILD"}]}
                """;
        service.validateAndSyncEdges(PARENT_VERSION_ID, PARENT_ID, "MOD-PARENT", structure, author);

        ArgumentCaptor<ContentModuleNestingEdgeEntity> captor =
                ArgumentCaptor.forClass(ContentModuleNestingEdgeEntity.class);
        verify(edgeRepository).deleteByParentVersionId(PARENT_VERSION_ID);
        verify(edgeRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetModuleId()).isEqualTo(CHILD_ID);
        assertThat(captor.getValue().getReferenceKey()).isEqualTo("MOD-CHILD");
    }

    @Test
    void validateAndSyncEdges_selfCycle_rejected() {
        String structure = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"MOD-PARENT"}]}
                """;
        assertThatThrownBy(() ->
                service.validateAndSyncEdges(PARENT_VERSION_ID, PARENT_ID, "MOD-PARENT", structure, author))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo(ApiErrorCodes.CONTENT_MODULE_NESTING_CYCLE);
        verify(edgeRepository, never()).save(any());
    }

    @Test
    void validateAndSyncEdges_unresolvedTarget_rejected() {
        when(accessSupport.resolveModule("MISSING")).thenReturn(Optional.empty());
        String structure = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"MISSING"}]}
                """;
        assertThatThrownBy(() ->
                service.validateAndSyncEdges(PARENT_VERSION_ID, PARENT_ID, "MOD-PARENT", structure, author))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo(ApiErrorCodes.CONTENT_MODULE_NESTING_TARGET_UNRESOLVED);
    }

    @Test
    void validateAndSyncEdges_sameTargetMultipleReferenceKeys_dedupesToOneEdge() {
        ContentModuleEntity aliasChild = new ContentModuleEntity(
                CHILD_ID, "MOD-CHILD-ALIAS", "RETAIL", "Child Alias", "d", "[]", "10000003");
        when(accessSupport.resolveModule("MOD-CHILD")).thenReturn(Optional.of(child));
        when(accessSupport.resolveModule("MOD-CHILD-ALIAS")).thenReturn(Optional.of(aliasChild));
        when(accessSupport.canAccessModule(author, child)).thenReturn(true);
        when(accessSupport.canAccessModule(author, aliasChild)).thenReturn(true);
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(CHILD_ID))
                .thenReturn(List.of(new ContentModuleVersionEntity(
                        CHILD_VERSION_ID, CHILD_ID, "1.0.0", "{\"nodes\":[]}", "i", "10000003")));
        when(edgeRepository.findByParentVersionId(CHILD_VERSION_ID)).thenReturn(List.of());

        String structure = """
                {"nodes":[
                  {"type":"contentModuleRef","referenceKey":"MOD-CHILD"},
                  {"type":"contentModuleRef","referenceKey":"MOD-CHILD-ALIAS"}
                ]}
                """;
        service.validateAndSyncEdges(PARENT_VERSION_ID, PARENT_ID, "MOD-PARENT", structure, author);

        ArgumentCaptor<ContentModuleNestingEdgeEntity> captor =
                ArgumentCaptor.forClass(ContentModuleNestingEdgeEntity.class);
        verify(edgeRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getTargetModuleId()).isEqualTo(CHILD_ID);
        assertThat(captor.getValue().getReferenceKey()).isEqualTo("MOD-CHILD");
    }

    @Test
    void validateAndSyncEdges_malformedStructureJson_rejected() {
        assertThatThrownBy(() ->
                service.validateAndSyncEdges(
                        PARENT_VERSION_ID, PARENT_ID, "MOD-PARENT", "{not-valid-json", author))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo(ApiErrorCodes.CONTENT_MODULE_NESTING_STRUCTURE_INVALID);
        verify(edgeRepository, never()).deleteByParentVersionId(any());
        verify(edgeRepository, never()).save(any());
    }

    @Test
    void validateAndSyncEdges_mutualCycle_rejected() {
        when(accessSupport.resolveModule("MOD-CHILD")).thenReturn(Optional.of(child));
        when(accessSupport.canAccessModule(author, child)).thenReturn(true);
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(CHILD_ID))
                .thenReturn(List.of(new ContentModuleVersionEntity(
                        CHILD_VERSION_ID, CHILD_ID, "1.0.0", "{\"nodes\":[]}", "i", "10000003")));
        when(edgeRepository.findByParentVersionId(CHILD_VERSION_ID)).thenReturn(List.of(
                new ContentModuleNestingEdgeEntity(
                        UUID.randomUUID(), CHILD_VERSION_ID, PARENT_ID, "MOD-PARENT")));

        String structure = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"MOD-CHILD"}]}
                """;
        assertThatThrownBy(() ->
                service.validateAndSyncEdges(PARENT_VERSION_ID, PARENT_ID, "MOD-PARENT", structure, author))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo(ApiErrorCodes.CONTENT_MODULE_NESTING_CYCLE);
    }

    @Test
    void validateAndSyncEdges_depthNine_rejected() {
        UUID[] moduleIds = new UUID[10];
        for (int i = 0; i < 10; i++) {
            moduleIds[i] = UUID.nameUUIDFromBytes(("mod-" + i).getBytes());
        }
        ContentModuleEntity m1 = new ContentModuleEntity(
                moduleIds[1], "MOD-1", "RETAIL", "M1", "d", "[]", "10000003");
        when(accessSupport.resolveModule("MOD-1")).thenReturn(Optional.of(m1));
        when(accessSupport.canAccessModule(author, m1)).thenReturn(true);

        for (int i = 1; i <= 8; i++) {
            UUID versionId = UUID.nameUUIDFromBytes(("ver-" + i).getBytes());
            when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(moduleIds[i]))
                    .thenReturn(List.of(new ContentModuleVersionEntity(
                            versionId, moduleIds[i], "1.0.0", "{}", "i", "10000003")));
            when(edgeRepository.findByParentVersionId(versionId)).thenReturn(List.of(
                    new ContentModuleNestingEdgeEntity(
                            UUID.randomUUID(),
                            versionId,
                            moduleIds[i + 1],
                            "MOD-" + (i + 1))));
        }

        String structure = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"MOD-1"}]}
                """;
        assertThatThrownBy(() ->
                service.validateAndSyncEdges(PARENT_VERSION_ID, moduleIds[0], "MOD-0", structure, author))
                .isInstanceOf(ContentModuleGovernanceException.class)
                .extracting(ex -> ((ContentModuleGovernanceException) ex).errorCode())
                .isEqualTo(ApiErrorCodes.CONTENT_MODULE_NESTING_DEPTH_EXCEEDED);
    }

    @Test
    void validateAndSyncEdges_depthEight_accepted() {
        UUID[] moduleIds = new UUID[9];
        for (int i = 0; i < 9; i++) {
            moduleIds[i] = UUID.nameUUIDFromBytes(("d8-mod-" + i).getBytes());
        }
        ContentModuleEntity m1 = new ContentModuleEntity(
                moduleIds[1], "MOD-1", "RETAIL", "M1", "d", "[]", "10000003");
        when(accessSupport.resolveModule("MOD-1")).thenReturn(Optional.of(m1));
        when(accessSupport.canAccessModule(author, m1)).thenReturn(true);

        for (int i = 1; i <= 7; i++) {
            UUID versionId = UUID.nameUUIDFromBytes(("d8-ver-" + i).getBytes());
            when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(moduleIds[i]))
                    .thenReturn(List.of(new ContentModuleVersionEntity(
                            versionId, moduleIds[i], "1.0.0", "{}", "i", "10000003")));
            when(edgeRepository.findByParentVersionId(versionId)).thenReturn(List.of(
                    new ContentModuleNestingEdgeEntity(
                            UUID.randomUUID(),
                            versionId,
                            moduleIds[i + 1],
                            "MOD-" + (i + 1))));
        }
        UUID leafVersion = UUID.nameUUIDFromBytes("d8-ver-8".getBytes());
        when(versionRepository.findByModuleIdOrderBySemanticVersionDesc(moduleIds[8]))
                .thenReturn(List.of(new ContentModuleVersionEntity(
                        leafVersion, moduleIds[8], "1.0.0", "{}", "i", "10000003")));
        when(edgeRepository.findByParentVersionId(leafVersion)).thenReturn(List.of());

        String structure = """
                {"nodes":[{"type":"contentModuleRef","referenceKey":"MOD-1"}]}
                """;
        service.validateAndSyncEdges(PARENT_VERSION_ID, moduleIds[0], "MOD-0", structure, author);
        verify(edgeRepository).save(any());
    }

    @Test
    void evaluatePublishClosure_missingNestedPin_blocks() {
        Map<String, String> pins = Map.of(
                "PARENT",
                "{\"nodes\":[{\"type\":\"contentModuleRef\",\"referenceKey\":\"CHILD\"}]}"
        );
        ContentModuleNestingPublishSummaryView summary = service.evaluatePublishClosure(pins);
        assertThat(summary.unpinnedBlocking()).isTrue();
        assertThat(summary.cycleBlocking()).isFalse();
        assertThat(summary.depthBlocking()).isFalse();
    }

    @Test
    void evaluatePublishClosure_cycle_blocks() {
        Map<String, String> pins = Map.of(
                "A", "{\"nodes\":[{\"type\":\"contentModuleRef\",\"referenceKey\":\"B\"}]}",
                "B", "{\"nodes\":[{\"type\":\"contentModuleRef\",\"referenceKey\":\"A\"}]}"
        );
        ContentModuleNestingPublishSummaryView summary = service.evaluatePublishClosure(pins);
        assertThat(summary.cycleBlocking()).isTrue();
    }

    @Test
    void evaluatePublishClosure_completeTransitivePins_passes() {
        Map<String, String> pins = Map.of(
                "PARENT",
                "{\"nodes\":[{\"type\":\"contentModuleRef\",\"referenceKey\":\"CHILD\"}]}",
                "CHILD",
                "{\"nodes\":[{\"type\":\"paragraph\",\"children\":[]}]}"
        );
        ContentModuleNestingPublishSummaryView summary = service.evaluatePublishClosure(pins);
        assertThat(summary.cycleBlocking()).isFalse();
        assertThat(summary.depthBlocking()).isFalse();
        assertThat(summary.unpinnedBlocking()).isFalse();
    }
}
