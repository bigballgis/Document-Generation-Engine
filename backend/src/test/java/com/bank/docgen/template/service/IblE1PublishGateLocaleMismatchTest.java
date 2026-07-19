package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.template.api.ContentModuleLocaleMismatchSummaryView;
import com.bank.docgen.template.api.PublishGateItemView;
import com.bank.docgen.template.domain.PublishGateCheckCode;
import com.bank.docgen.template.port.PreviewEvidencePort;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-IBL-E1-007 / 008 — publish gate CONTENT_MODULE_LOCALE_MISMATCH.
 */
@ExtendWith(MockitoExtension.class)
class IblE1PublishGateLocaleMismatchTest {

    @Mock
    private PreviewEvidencePort previewEvidencePort;
    @Mock
    private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock
    private AnchorBindingRepository anchorBindingRepository;

    private PublishGateCheckItemContentSupport support;
    private UUID versionId;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        support = new PublishGateCheckItemContentSupport(
                previewEvidencePort,
                contentModuleReferenceService,
                anchorBindingRepository,
                new com.bank.docgen.authoring.structured.NodeMatrixValidationService(objectMapper),
                objectMapper
        );
        versionId = UUID.randomUUID();
    }

    @Test
    void contentModuleLocaleMismatch_blocksWhenIncompatible_bddE1007() {
        when(contentModuleReferenceService.evaluateLocaleMismatch(versionId))
                .thenReturn(new ContentModuleLocaleMismatchSummaryView(
                        true,
                        1,
                        1,
                        List.of("MOD-ZH@1.0.0 locale=zh-CN")
                ));

        PublishGateItemView item = support.contentModuleLocaleMismatchItem(versionId);

        assertThat(item.checkCode()).isEqualTo(PublishGateCheckCode.CONTENT_MODULE_LOCALE_MISMATCH);
        assertThat(item.blocker()).isTrue();
        assertThat(item.ready()).isFalse();
        assertThat(item.messageKey()).isEqualTo("api.publishGate.contentModuleLocaleMismatch.blocked");
        assertThat(item.summary()).contains("MOD-ZH");
    }

    @Test
    void contentModuleLocaleMismatch_passesWhenCompatibleRegionDiffers_bddE1008() {
        when(contentModuleReferenceService.evaluateLocaleMismatch(versionId))
                .thenReturn(new ContentModuleLocaleMismatchSummaryView(false, 0, 1, List.of()));

        PublishGateItemView item = support.contentModuleLocaleMismatchItem(versionId);

        assertThat(item.checkCode()).isEqualTo(PublishGateCheckCode.CONTENT_MODULE_LOCALE_MISMATCH);
        assertThat(item.blocker()).isFalse();
        assertThat(item.ready()).isTrue();
        assertThat(item.messageKey()).isEqualTo("api.publishGate.contentModuleLocaleMismatch.ready");
    }
}
