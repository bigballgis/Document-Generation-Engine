package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import com.bank.docgen.runtime.api.FidelityWarning;
import com.bank.docgen.sharedkernel.document.fidelity.FidelityWarningCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuntimeFidelityWarningMapperTest {

    @Mock
    private MessageResolver messageResolver;

    private RuntimeFidelityWarningMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RuntimeFidelityWarningMapper(messageResolver);
        lenient().when(messageResolver.resolveOrDefault(anyString(), anyString()))
                .thenAnswer(inv -> "Resolved: " + inv.getArgument(0));
    }

    @Test
    void mapsWarningCodesToOpenApiShapedObjectsPreservingOrder() {
        List<FidelityWarning> warnings = mapper.toWarnings(List.of(
                FidelityWarningCode.CONTROLLED_STYLE_FALLBACK.name(),
                FidelityWarningCode.MASTER_STYLE_FALLBACK.name()
        ));

        assertThat(warnings).hasSize(2);
        assertThat(warnings.get(0).warningCode()).isEqualTo("CONTROLLED_STYLE_FALLBACK");
        assertThat(warnings.get(0).messageKey())
                .isEqualTo("generation.warning.fidelity.controlledStyleFallback");
        assertThat(warnings.get(0).message()).isEqualTo(
                "Resolved: generation.warning.fidelity.controlledStyleFallback");
        assertThat(warnings.get(1).warningCode()).isEqualTo("MASTER_STYLE_FALLBACK");
        assertThat(warnings.get(1).sensitiveDataExcluded()).isTrue();
    }

    @Test
    void nullOrEmptyCodesYieldEmptyList() {
        assertThat(mapper.toWarnings(null)).isEmpty();
        assertThat(mapper.toWarnings(List.of())).isEmpty();
    }

    @Test
    void warningCodesForHeadersMatchJsonWarningCodes() {
        List<String> codes = List.of(
                FidelityWarningCode.IMAGE_SCALING_ADJUSTED.name(),
                FidelityWarningCode.PDF_PAGE_NUMBER_STAMP_FAILED.name()
        );
        List<FidelityWarning> warnings = mapper.toWarnings(codes);

        assertThat(mapper.toWarningCodes(warnings)).containsExactlyElementsOf(codes);
        assertThat(warnings).hasSize(2);
    }

    @Test
    void usesDefaultMessageWhenResolverFallsBack() {
        when(messageResolver.resolveOrDefault(
                eq("generation.warning.fidelity.controlledStyleFallback"),
                anyString()
        )).thenAnswer(inv -> inv.getArgument(1));

        FidelityWarning warning = mapper.toWarnings(List.of(
                FidelityWarningCode.CONTROLLED_STYLE_FALLBACK.name()
        )).getFirst();

        assertThat(warning.message()).isNotBlank();
        assertThat(warning.locationSummary()).isNotBlank();
        assertThat(warning.detectedSummary()).isNotBlank();
        assertThat(warning.recommendation()).isNotBlank();
    }
}
