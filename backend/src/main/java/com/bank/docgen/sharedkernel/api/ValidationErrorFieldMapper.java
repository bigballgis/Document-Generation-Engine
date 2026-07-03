package com.bank.docgen.sharedkernel.api;

import com.bank.docgen.infrastructure.i18n.MessageResolver;
import org.springframework.stereotype.Component;

@Component
public class ValidationErrorFieldMapper {

    private final MessageResolver messageResolver;

    public ValidationErrorFieldMapper(MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    public FieldError toFieldError(org.springframework.validation.FieldError error) {
        String messageKey = validationMessageKey(error);
        return new FieldError(
                error.getField(),
                validationReason(error),
                messageResolver.resolveOrDefault(messageKey, error.getDefaultMessage())
        );
    }

    private String validationMessageKey(org.springframework.validation.FieldError error) {
        String code = error.getCode();
        if (code == null) {
            return "api.error.validation.fieldInvalid";
        }
        if (isRequiredConstraint(code)) {
            return "api.error.validation.fieldRequired";
        }
        if ("Size".equals(code)) {
            return "api.error.validation.fieldSizeInvalid";
        }
        if ("Pattern".equals(code)) {
            return "api.error.validation.fieldPatternInvalid";
        }
        return "api.error.validation.fieldInvalid";
    }

    private boolean isRequiredConstraint(String code) {
        return "NotBlank".equals(code) || "NotNull".equals(code) || "NotEmpty".equals(code);
    }

    private String validationReason(org.springframework.validation.FieldError error) {
        String code = error.getCode();
        if (code == null) {
            return "RULE_FAILED";
        }
        if (isRequiredConstraint(code)) {
            return "REQUIRED";
        }
        if ("Size".equals(code)) {
            Object[] arguments = error.getArguments();
            if (arguments != null && arguments.length >= 2
                    && error.getRejectedValue() instanceof String rejected
                    && rejected.length() > ((Number) arguments[1]).intValue()) {
                return "TOO_LONG";
            }
            return "TOO_SHORT";
        }
        if ("Pattern".equals(code)) {
            return "PATTERN_MISMATCH";
        }
        return "RULE_FAILED";
    }
}
