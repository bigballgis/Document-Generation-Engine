package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.template.api.CompositionInclusionMatchView;
import com.bank.docgen.template.api.CompositionInclusionRuleView;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.port.CompositionInclusionAxes;
import com.bank.docgen.template.port.CompositionInclusionUnsatisfiedException;
import com.bank.docgen.template.port.ContentModuleJurisdictionMismatchException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ADR-0063 Decision 7 / BDD E2-C9 — preview/test must evaluate inclusion with real axes
 * (same evaluator as runtime), not {@link CompositionInclusionAxes#empty()}.
 */
@ExtendWith(MockitoExtension.class)
class TemplateRenderContextAdapterInclusionAxesTest {

    private static final UUID VERSION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID TEMPLATE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Mock
    private TemplateCurrentVersionResolver templateCurrentVersionResolver;
    @Mock
    private TemplateContentModuleReferenceService contentModuleReferenceService;
    @Mock
    private CompositionInclusionRuleService compositionInclusionRuleService;
    @Mock
    private TemplateVersionRepository templateVersionRepository;

    private TemplateRenderContextAdapter adapter;
    private TemplateVersionEntity version;

    @BeforeEach
    void setUp() {
        adapter = new TemplateRenderContextAdapter(
                templateCurrentVersionResolver,
                contentModuleReferenceService,
                compositionInclusionRuleService,
                templateVersionRepository
        );
        version = new TemplateVersionEntity(VERSION_ID, TEMPLATE_ID, "10000001");
        when(templateVersionRepository.findById(VERSION_ID)).thenReturn(java.util.Optional.of(version));
        when(contentModuleReferenceService.resolvePinnedContentStructures(VERSION_ID)).thenReturn(Map.of(
                "ref-hk-law", "{\"type\":\"paragraph\",\"text\":\"HK\"}",
                "ref-uk-law", "{\"type\":\"paragraph\",\"text\":\"UK\"}",
                "ref-common", "{\"type\":\"paragraph\",\"text\":\"COMMON\"}"
        ));
        when(contentModuleReferenceService.resolvePinnedJurisdictions(VERSION_ID)).thenReturn(Map.of(
                "ref-hk-law", "Hong Kong",
                "ref-uk-law", "England and Wales"
        ));
        when(compositionInclusionRuleService.loadRules(version)).thenReturn(List.of(
                rule("R-HK", "ref-hk-law", "Hong Kong", null, null, 0, false),
                rule("R-UK", "ref-uk-law", "England and Wales", null, null, 0, false)
        ));
    }

    @Test
    void e2c9_previewAxes_includeMatchingAndExcludeNonMatching_sameAsRuntime() {
        Map<String, String> pinned = adapter.resolvePinnedContentStructures(
                VERSION_ID,
                CompositionInclusionAxes.of("Hong Kong", "TRADE-LC", "API")
        );

        assertThat(pinned).containsOnlyKeys("ref-hk-law", "ref-common");
        assertThat(pinned).doesNotContainKey("ref-uk-law");
    }

    @Test
    void e2c9_emptyAxesWouldWronglyExclude_whenRulesRequireJurisdiction() {
        // Document the bug class: empty axes must not be substituted for a real Hong Kong request.
        Map<String, String> withEmpty = adapter.resolvePinnedContentStructures(
                VERSION_ID,
                CompositionInclusionAxes.empty()
        );
        Map<String, String> withHk = adapter.resolvePinnedContentStructures(
                VERSION_ID,
                CompositionInclusionAxes.of("Hong Kong", null, null)
        );

        assertThat(withEmpty).containsOnlyKeys("ref-common");
        assertThat(withHk).containsKeys("ref-hk-law", "ref-common");
        assertThat(withHk).doesNotContainKey("ref-uk-law");
    }

    @Test
    void e2c9_requiredInclusionUnsatisfied_throwsStableCode() {
        when(compositionInclusionRuleService.loadRules(version)).thenReturn(List.of(
                rule("R-HK-REQ", "ref-hk-law", "Hong Kong", null, null, 0, true)
        ));

        assertThatThrownBy(() -> adapter.resolvePinnedContentStructures(
                VERSION_ID,
                CompositionInclusionAxes.of("England and Wales", null, null)
        ))
                .isInstanceOf(CompositionInclusionUnsatisfiedException.class)
                .satisfies(ex -> assertThat(((CompositionInclusionUnsatisfiedException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.COMPOSITION_INCLUSION_UNSATISFIED));
    }

    @Test
    void e2c10_jurisdictionMismatch_throwsStableCode() {
        when(compositionInclusionRuleService.loadRules(version)).thenReturn(List.of());
        when(contentModuleReferenceService.resolvePinnedContentStructures(VERSION_ID)).thenReturn(Map.of(
                "ref-hk-law", "{\"type\":\"paragraph\",\"text\":\"HK\"}"
        ));
        when(contentModuleReferenceService.resolvePinnedJurisdictions(VERSION_ID)).thenReturn(Map.of(
                "ref-hk-law", "Hong Kong"
        ));

        assertThatThrownBy(() -> adapter.resolvePinnedContentStructures(
                VERSION_ID,
                CompositionInclusionAxes.of("England and Wales", null, null)
        ))
                .isInstanceOf(ContentModuleJurisdictionMismatchException.class)
                .satisfies(ex -> assertThat(((ContentModuleJurisdictionMismatchException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.CONTENT_MODULE_JURISDICTION_MISMATCH));
    }

    private static CompositionInclusionRuleView rule(
            String ruleId,
            String referenceKey,
            String jurisdiction,
            String product,
            String channel,
            int priority,
            boolean required
    ) {
        return new CompositionInclusionRuleView(
                ruleId,
                referenceKey,
                new CompositionInclusionMatchView(jurisdiction, product, channel),
                priority,
                required
        );
    }
}
