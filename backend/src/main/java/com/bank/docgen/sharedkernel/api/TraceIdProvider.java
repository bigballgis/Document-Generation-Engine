package com.bank.docgen.sharedkernel.api;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TraceIdProvider {

    public String currentOrNew(String incomingTraceId) {
        if (incomingTraceId != null && !incomingTraceId.isBlank()) {
            return incomingTraceId.trim();
        }
        return UUID.randomUUID().toString();
    }

    public String newAuditId() {
        return "AUD-" + UUID.randomUUID();
    }
}
