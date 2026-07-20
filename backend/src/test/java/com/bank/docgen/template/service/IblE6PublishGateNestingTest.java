package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bank.docgen.contentmodule.api.ContentModuleNestingPublishSummaryView;
import com.bank.docgen.template.api.PublishGateItemView;
import com.bank.docgen.template.domain.PublishGateCheckCode;
import com.bank.docgen.template.port.PreviewEvidencePort;
import com.bank.docgen.authoring.structured.NodeMatrixValidationService;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IblE6PublishGateNestingTest {

    private static final UUID VERSION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock private PreviewEvidencePort previewEvidencePort;
    @Mock private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock private AnchorBindingRepository anchorBindingRepository;
    @Mock private NodeMatrixValidationService nodeMatrixValidationService;

    private PublishGateCheckItemContentSupport contentItems;

    @BeforeEach
    void setUp() {
        contentItems = new PublishGateCheckItemContentSupport(
                previewEvidencePort,
                contentModuleReferenceService,
                anchorBindingRepository,
                nodeMatrixValidationService,
                new ObjectMapper()
        );
    }

    @Test
    void nestingItems_unpinnedNestedPin_fails() {
        when(contentModuleReferenceService.evaluateNestingClosure(VERSION_ID))
                .thenReturn(new ContentModuleNestingPublishSummaryView(
                        false,
                        false,
                        true,
                        List.of(),
                        List.of(),
                        List.of("missingPin=CHILD,from=PARENT")));

        List<PublishGateItemView> items = contentItems.contentModuleNestingItems(VERSION_ID);

        assertThat(items).anyMatch(item ->
                item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_NESTING_UNPINNED && item.blocker());
        assertThat(items).noneMatch(item ->
                item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_NESTING_CYCLE && item.blocker());
    }

    @Test
    void nestingItems_cycle_fails() {
        when(contentModuleReferenceService.evaluateNestingClosure(any()))
                .thenReturn(new ContentModuleNestingPublishSummaryView(
                        true, false, false, List.of("cycleAt=A"), List.of(), List.of()));

        List<PublishGateItemView> items = contentItems.contentModuleNestingItems(VERSION_ID);

        assertThat(items).anyMatch(item ->
                item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_NESTING_CYCLE && item.blocker());
    }

    @Test
    void nestingItems_depthExceeded_fails() {
        when(contentModuleReferenceService.evaluateNestingClosure(VERSION_ID))
                .thenReturn(new ContentModuleNestingPublishSummaryView(
                        false, true, false, List.of(), List.of("depth=9,at=LEAF"), List.of()));

        List<PublishGateItemView> items = contentItems.contentModuleNestingItems(VERSION_ID);

        assertThat(items).anyMatch(item ->
                item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_NESTING_DEPTH_EXCEEDED
                        && item.blocker());
    }
}
