package com.bank.docgen.template.domain;

/**
 * Optional PII classification on a template variable schema field (CE-G03).
 * {@link #NONE} / omitted = not PII-governed for test-data-set save gates.
 */
public enum VariablePiiCategory {
    NONE,
    PERSONAL_NAME,
    GOVERNMENT_ID,
    FINANCIAL_ACCOUNT,
    CONTACT,
    ADDRESS,
    OTHER_SENSITIVE
}
