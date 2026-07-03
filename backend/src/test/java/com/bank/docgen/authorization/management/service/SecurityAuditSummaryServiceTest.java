package com.bank.docgen.authorization.management.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecurityAuditSummaryServiceTest {

    @Mock
    private com.bank.docgen.audit.service.SecurityManagementAuditRecorder auditRecorder;

    @InjectMocks
    private SecurityAuditSummaryService service;

    @Test
    void recordLoginFailurePersistsDurableAuditEvent() {
        service.recordLoginFailure("10000001", "AUD-1", "trace-1");

        verify(auditRecorder).recordSecurityLoginFailure("10000001", "AUD-1", "trace-1");
    }
}
