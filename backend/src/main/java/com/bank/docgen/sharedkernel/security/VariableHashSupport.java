package com.bank.docgen.sharedkernel.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

public final class VariableHashSupport {

    private VariableHashSupport() {
    }

    public static String hashVariables(ObjectMapper objectMapper, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return hashPayload(objectMapper, Map.of());
        }
        return hashPayload(objectMapper, variables);
    }

    public static String hashPayload(ObjectMapper objectMapper, Object payload) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception ex) {
            return "unknown";
        }
    }
}
