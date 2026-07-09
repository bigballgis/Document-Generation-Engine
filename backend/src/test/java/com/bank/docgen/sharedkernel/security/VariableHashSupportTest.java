package com.bank.docgen.sharedkernel.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VariableHashSupportTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void hashVariablesIsStableSha256Hex() {
        String first = VariableHashSupport.hashVariables(objectMapper, Map.of("accountNo", "1234567890"));
        String second = VariableHashSupport.hashVariables(objectMapper, Map.of("accountNo", "1234567890"));

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }
}
