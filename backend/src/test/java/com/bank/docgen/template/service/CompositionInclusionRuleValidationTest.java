package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.template.api.CompositionInclusionMatchView;
import com.bank.docgen.template.api.CompositionInclusionRuleView;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * BDD-IBL-E2-012 — PUT validation for unknown referenceKey / empty match / duplicate ruleId.
 */
class CompositionInclusionRuleValidationTest {

    @Test
    void unknownReferenceKey_throwsRuleInvalid_bddE2012() {
        var rules = List.of(new CompositionInclusionRuleView(
                "R1",
                "ref-missing",
                new CompositionInclusionMatchView("Hong Kong", null, null),
                0,
                false
        ));

        assertThatThrownBy(() -> CompositionInclusionRuleValidator.validate(rules, Set.of("ref-hk")))
                .isInstanceOf(CompositionInclusionRuleInvalidException.class)
                .satisfies(ex -> assertThat(((CompositionInclusionRuleInvalidException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.COMPOSITION_INCLUSION_RULE_INVALID));
    }

    @Test
    void emptyMatch_throwsRuleInvalid() {
        var rules = List.of(new CompositionInclusionRuleView(
                "R1",
                "ref-hk",
                new CompositionInclusionMatchView("  ", null, null),
                0,
                false
        ));

        assertThatThrownBy(() -> CompositionInclusionRuleValidator.validate(rules, Set.of("ref-hk")))
                .isInstanceOf(CompositionInclusionRuleInvalidException.class);
    }

    @Test
    void duplicateRuleId_throwsRuleInvalid() {
        var match = new CompositionInclusionMatchView("Hong Kong", null, null);
        var rules = List.of(
                new CompositionInclusionRuleView("R1", "ref-hk", match, 0, false),
                new CompositionInclusionRuleView("R1", "ref-hk", match, 1, false)
        );

        assertThatThrownBy(() -> CompositionInclusionRuleValidator.validate(rules, Set.of("ref-hk")))
                .isInstanceOf(CompositionInclusionRuleInvalidException.class);
    }

    @Test
    void validRules_pass() {
        var rules = List.of(new CompositionInclusionRuleView(
                "R1",
                "ref-hk",
                new CompositionInclusionMatchView("Hong Kong", null, null),
                0,
                false
        ));

        CompositionInclusionRuleValidator.validate(rules, Set.of("ref-hk"));
    }
}
