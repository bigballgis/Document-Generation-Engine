package com.bank.docgen.rendering.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.rendering.api.FidelityWarningView;
import com.bank.docgen.rendering.api.PreviewComparisonView;
import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import com.bank.docgen.authoring.structured.ReferenceNodeService;
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.domain.PreviewComparisonLocationType;
import com.bank.docgen.template.domain.PreviewComparisonSeverity;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PreviewComparisonServiceTest {

    private PreviewComparisonService service;

    @BeforeEach
    void setUp() {
        service = new PreviewComparisonService();
    }

    @Test
    void comparison_locatesAnchorLevelDiff() {
        AnchorBindingEntity binding = new AnchorBindingEntity(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "HEADER",
                AnchorContentType.TEXT,
                "{}",
                BindingValidationStatus.MISSING_ANCHOR
        );

        PreviewComparisonView comparison = service.compare(List.of(binding), List.of());

        assertThat(comparison.items()).anySatisfy(item ->
                assertThat(item.locationType()).isEqualTo(PreviewComparisonLocationType.ANCHOR)
        );
        assertThat(comparison.items().get(0).locationRef()).isEqualTo("HEADER");
    }

    @Test
    void comparison_semanticDiff_classifiedAsBlocker() {
        List<FidelityWarningView> warnings = List.of(
                new FidelityWarningView(
                        FidelityWarningCode.UNRESOLVED_VARIABLE.name(),
                        "generation.warning.fidelity.unresolvedVariable"
                )
        );

        PreviewComparisonView comparison = service.compare(List.of(), warnings);

        assertThat(comparison.blockerCount()).isEqualTo(1);
        assertThat(comparison.items().get(0).severity()).isEqualTo(PreviewComparisonSeverity.BLOCKER);
    }

    @Test
    void comparison_lowRiskDiff_classifiedAsWarning() {
        List<FidelityWarningView> warnings = List.of(
                new FidelityWarningView(
                        FidelityWarningCode.IMAGE_SCALING_ADJUSTED.name(),
                        ReferenceNodeService.MESSAGE_KEY_IMAGE_SCALING
                )
        );

        PreviewComparisonView comparison = service.compare(List.of(), warnings);

        assertThat(comparison.warningCount()).isEqualTo(1);
        assertThat(comparison.items().get(0).severity()).isEqualTo(PreviewComparisonSeverity.WARNING);
    }
}
