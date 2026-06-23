package com.bank.docgen.template.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bank.docgen.template.domain.RuleValidationStatus;
import org.junit.jupiter.api.Test;

class TemplateRuleValidationServiceLogicTest {

    private final TemplateRuleValidationService service = new TemplateRuleValidationService(null, null, null, null, null);

    @Test
    void detectsMissingVariableInExpression() {
        RuleValidationStatus status = invokeValidateRule(
                "rule-1",
                "${missing} != null",
                "ANCHOR-1",
                java.util.Set.of("customerName"),
                java.util.Set.of("ANCHOR-1"),
                java.util.Set.of("rule-1")
        );
        assertEquals(RuleValidationStatus.MISSING_VARIABLE, status);
    }

    @Test
    void acceptsValidRuleReferences() {
        RuleValidationStatus status = invokeValidateRule(
                "rule-1",
                "${customerName} != null",
                "ANCHOR-1",
                java.util.Set.of("customerName"),
                java.util.Set.of("ANCHOR-1"),
                java.util.Set.of("rule-1")
        );
        assertEquals(RuleValidationStatus.VALID, status);
    }

    @Test
    void flagsInvalidBranchReference() {
        RuleValidationStatus status = invokeValidateRule(
                "rule-1",
                "${customerName} != null",
                "ANCHOR-1",
                java.util.Set.of("customerName"),
                java.util.Set.of("ANCHOR-1"),
                java.util.Set.of("rule-1"),
                "missing-branch",
                null
        );
        assertEquals(RuleValidationStatus.INVALID_BRANCH_REFERENCE, status);
    }

    private RuleValidationStatus invokeValidateRule(
            String ruleId,
            String expression,
            String anchorId,
            java.util.Set<String> variables,
            java.util.Set<String> anchors,
            java.util.Set<String> ruleIds
    ) {
        return invokeValidateRule(ruleId, expression, anchorId, variables, anchors, ruleIds, null, null);
    }

    private RuleValidationStatus invokeValidateRule(
            String ruleId,
            String expression,
            String anchorId,
            java.util.Set<String> variables,
            java.util.Set<String> anchors,
            java.util.Set<String> ruleIds,
            String trueBranch,
            String falseBranch
    ) {
        try {
            var method = TemplateRuleValidationService.class.getDeclaredMethod(
                    "validateRule",
                    com.bank.docgen.template.api.TemplateRuleValidationItemRequest.class,
                    java.util.Set.class,
                    java.util.Set.class,
                    java.util.Set.class
            );
            method.setAccessible(true);
            var request = new com.bank.docgen.template.api.TemplateRuleValidationItemRequest(
                    ruleId,
                    expression,
                    anchorId,
                    trueBranch,
                    falseBranch
            );
            return (RuleValidationStatus) method.invoke(service, request, variables, anchors, ruleIds);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
