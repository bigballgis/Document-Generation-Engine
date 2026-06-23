package com.bank.docgen.template.domain;

public enum RuleValidationStatus {
    VALID,
    MISSING_VARIABLE,
    MISSING_ANCHOR,
    INVALID_BRANCH_REFERENCE,
    MALFORMED_RULE
}
