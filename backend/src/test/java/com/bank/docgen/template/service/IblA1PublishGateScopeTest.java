package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.template.domain.PublishGateCheckCode;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * BDD-IBL-A1-006 — publish checklist stays scoped out of generate-body VariableSchema validation.
 */
class IblA1PublishGateScopeTest {

    @Test
    void publishGateCheckCodes_doNotIncludeGenerateBodyVariableValidation() {
        Set<String> codes = Arrays.stream(PublishGateCheckCode.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(codes).contains(
                PublishGateCheckCode.VARIABLE_SCHEMA.name(),
                PublishGateCheckCode.PREVIEW_PRESENT.name()
        );
        assertThat(codes).noneMatch(code ->
                code.contains("GENERATE_BODY")
                        || code.contains("VARIABLE_VALIDATION")
                        || code.contains("REQUEST_VARIABLE")
        );
    }
}
