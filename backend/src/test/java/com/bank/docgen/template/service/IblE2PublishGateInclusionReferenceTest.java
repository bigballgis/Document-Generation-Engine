package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bank.docgen.authoring.structured.NodeMatrixValidationService;
import com.bank.docgen.template.api.CompositionInclusionMatchView;
import com.bank.docgen.template.api.CompositionInclusionRuleView;
import com.bank.docgen.template.api.PublishGateItemView;
import com.bank.docgen.template.domain.PublishGateCheckCode;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.port.PreviewEvidencePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * BDD-IBL-E2-013 — publish gate COMPOSITION_INCLUSION_REFERENCE_INVALID.
 */
class IblE2PublishGateInclusionReferenceTest {

    @Test
    void danglingInclusionReference_blocksPublish_bddE2013() {
        UUID versionId = UUID.randomUUID();
        TemplateVersionEntity version = new TemplateVersionEntity(versionId, UUID.randomUUID(), "author");
        version.setCompositionInclusionRulesJson("""
                [{"ruleId":"R1","referenceKey":"ref-gone","match":{"jurisdiction":"Hong Kong"},"priority":0,"requiredInclusion":false}]
                """);

        TemplateContentModuleReferenceService refs = mock(TemplateContentModuleReferenceService.class);
        when(refs.listReferenceKeys(versionId)).thenReturn(Set.of("ref-hk"));

        PublishGateCheckItemContentSupport support = new PublishGateCheckItemContentSupport(
                mock(PreviewEvidencePort.class),
                refs,
                mock(AnchorBindingRepository.class),
                mock(NodeMatrixValidationService.class),
                new ObjectMapper()
        );

        PublishGateItemView item = support.compositionInclusionReferenceItem(version);

        assertThat(item.checkCode()).isEqualTo(PublishGateCheckCode.COMPOSITION_INCLUSION_REFERENCE_INVALID);
        assertThat(item.blocker()).isTrue();
        assertThat(item.ready()).isFalse();
    }

    @Test
    void validInclusionReferences_passGate() {
        UUID versionId = UUID.randomUUID();
        TemplateVersionEntity version = new TemplateVersionEntity(versionId, UUID.randomUUID(), "author");
        List<CompositionInclusionRuleView> rules = List.of(new CompositionInclusionRuleView(
                "R1",
                "ref-hk",
                new CompositionInclusionMatchView("Hong Kong", null, null),
                0,
                false
        ));
        try {
            version.setCompositionInclusionRulesJson(new ObjectMapper().writeValueAsString(rules));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }

        TemplateContentModuleReferenceService refs = mock(TemplateContentModuleReferenceService.class);
        when(refs.listReferenceKeys(versionId)).thenReturn(Set.of("ref-hk"));

        PublishGateCheckItemContentSupport support = new PublishGateCheckItemContentSupport(
                mock(PreviewEvidencePort.class),
                refs,
                mock(AnchorBindingRepository.class),
                mock(NodeMatrixValidationService.class),
                new ObjectMapper()
        );

        PublishGateItemView item = support.compositionInclusionReferenceItem(version);

        assertThat(item.blocker()).isFalse();
        assertThat(item.ready()).isTrue();
    }
}
