package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import org.springframework.http.HttpStatus;

/**
 * ADR-0063 / IBL-E2 — illegal inclusion rule set on management PUT.
 */
public class CompositionInclusionRuleInvalidException extends TemplateGovernanceException {

    public CompositionInclusionRuleInvalidException(String messageKey) {
        super(ApiErrorCodes.COMPOSITION_INCLUSION_RULE_INVALID, messageKey, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
