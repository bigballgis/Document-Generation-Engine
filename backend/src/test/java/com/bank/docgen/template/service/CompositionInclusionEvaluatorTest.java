package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.template.api.CompositionInclusionMatchView;
import com.bank.docgen.template.api.CompositionInclusionRuleView;
import com.bank.docgen.template.domain.CompositionInclusionDecision;
import com.bank.docgen.template.port.CompositionInclusionAxes;
import com.bank.docgen.template.port.CompositionInclusionUnsatisfiedException;
import com.bank.docgen.template.port.ContentModuleJurisdictionMismatchException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * BDD-IBL-E2-003…008 — deterministic composition inclusion evaluation.
 */
class CompositionInclusionEvaluatorTest {

    @Test
    void noRules_defaultsInclude_bddE2003() {
        var result = CompositionInclusionEvaluator.evaluate(
                List.of("ref-a"),
                List.of(),
                CompositionInclusionAxes.of("Hong Kong", null, null)
        );

        assertThat(result.decisions()).hasSize(1);
        assertThat(result.decisions().getFirst().decision()).isEqualTo(CompositionInclusionDecision.INCLUDE);
        assertThat(result.decisions().getFirst().matchedRuleId())
                .isEqualTo(CompositionInclusionEvaluator.NONE_DEFAULT);
        assertThat(result.includedReferenceKeys()).containsExactly("ref-a");
    }

    @Test
    void matchingJurisdiction_isCaseInsensitive_bddE2004() {
        var rules = List.of(rule("R1", "ref-hk", match("Hong Kong", null, null), 0, false));

        var result = CompositionInclusionEvaluator.evaluate(
                List.of("ref-hk"),
                rules,
                CompositionInclusionAxes.of("hong kong", null, null)
        );

        assertThat(result.decisions().getFirst().decision()).isEqualTo(CompositionInclusionDecision.INCLUDE);
        assertThat(result.decisions().getFirst().matchedRuleId()).isEqualTo("R1");
    }

    @Test
    void rulesExistButNoneMatch_excludes_bddE2005() {
        var rules = List.of(rule("R1", "ref-hk", match("Hong Kong", null, null), 0, false));

        var result = CompositionInclusionEvaluator.evaluate(
                List.of("ref-hk"),
                rules,
                CompositionInclusionAxes.of("England and Wales", null, null)
        );

        assertThat(result.decisions().getFirst().decision()).isEqualTo(CompositionInclusionDecision.EXCLUDE);
        assertThat(result.includedReferenceKeys()).isEmpty();
    }

    @Test
    void requiredInclusionUnsatisfied_throws_bddE2006() {
        var rules = List.of(rule("R1", "ref-hk", match("Hong Kong", null, null), 0, true));

        assertThatThrownBy(() -> CompositionInclusionEvaluator.evaluate(
                List.of("ref-hk"),
                rules,
                CompositionInclusionAxes.of(null, null, null)
        ))
                .isInstanceOf(CompositionInclusionUnsatisfiedException.class)
                .satisfies(ex -> assertThat(((CompositionInclusionUnsatisfiedException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.COMPOSITION_INCLUSION_UNSATISFIED));
    }

    @Test
    void multiAxisAnd_requiresAllDeclaredAxes_bddE2007() {
        var rules = List.of(rule(
                "R2",
                "ref-hk",
                match("Hong Kong", "TRADE-LC", "API"),
                0,
                false
        ));

        var partial = CompositionInclusionEvaluator.evaluate(
                List.of("ref-hk"),
                rules,
                CompositionInclusionAxes.of("Hong Kong", "TRADE-LC", null)
        );
        assertThat(partial.decisions().getFirst().decision()).isEqualTo(CompositionInclusionDecision.EXCLUDE);

        var full = CompositionInclusionEvaluator.evaluate(
                List.of("ref-hk"),
                rules,
                CompositionInclusionAxes.of("Hong Kong", "TRADE-LC", "API")
        );
        assertThat(full.decisions().getFirst().decision()).isEqualTo(CompositionInclusionDecision.INCLUDE);
        assertThat(full.decisions().getFirst().matchedRuleId()).isEqualTo("R2");
    }

    @Test
    void orAcrossRules_firstMatchByPriorityThenRuleId_bddE2008() {
        var rules = List.of(
                rule("R-high", "ref-x", match("Hong Kong", null, null), 0, false),
                rule("R-low", "ref-x", match("Hong Kong", null, null), 10, false),
                rule("R-a", "ref-x", match("Hong Kong", null, null), 0, false)
        );

        var result = CompositionInclusionEvaluator.evaluate(
                List.of("ref-x"),
                rules,
                CompositionInclusionAxes.of("Hong Kong", null, null)
        );

        assertThat(result.decisions().getFirst().matchedRuleId()).isEqualTo("R-a");
    }

    @Test
    void jurisdictionMismatch_whenBothNonBlank_throws_bddE2009() {
        assertThatThrownBy(() -> CompositionInclusionEvaluator.assertJurisdictionCompatible(
                "Hong Kong",
                "England and Wales"
        ))
                .isInstanceOf(ContentModuleJurisdictionMismatchException.class)
                .satisfies(ex -> assertThat(((ContentModuleJurisdictionMismatchException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.CONTENT_MODULE_JURISDICTION_MISMATCH));
    }

    @Test
    void jurisdictionMismatch_skippedWhenEitherBlank_bddE2010() {
        CompositionInclusionEvaluator.assertJurisdictionCompatible(null, "Hong Kong");
        CompositionInclusionEvaluator.assertJurisdictionCompatible("Hong Kong", null);
        CompositionInclusionEvaluator.assertJurisdictionCompatible("  ", "Hong Kong");
        CompositionInclusionEvaluator.assertJurisdictionCompatible("Hong Kong", "hong kong");
    }

    private static CompositionInclusionRuleView rule(
            String ruleId,
            String referenceKey,
            CompositionInclusionMatchView match,
            int priority,
            boolean required
    ) {
        return new CompositionInclusionRuleView(ruleId, referenceKey, match, priority, required);
    }

    private static CompositionInclusionMatchView match(String jurisdiction, String product, String channel) {
        return new CompositionInclusionMatchView(jurisdiction, product, channel);
    }
}
