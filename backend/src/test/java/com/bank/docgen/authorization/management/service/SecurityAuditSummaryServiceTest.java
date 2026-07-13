package com.bank.docgen.authorization.management.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.bank.docgen.audit.service.SecurityManagementAuditRecorder;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-LRP-D7-001/002/003/005/006/007 — durable security audit summary + fail-safe.
 */
@ExtendWith(MockitoExtension.class)
class SecurityAuditSummaryServiceTest {

    @Mock
    private SecurityManagementAuditRecorder auditRecorder;

    @InjectMocks
    private SecurityAuditSummaryService service;

    @Test
    void recordLoginFailurePersistsDurableAuditEvent() {
        service.recordLoginFailure("10000001", "AUD-1", "trace-1");

        verify(auditRecorder).recordSecurityLoginFailure("10000001", "AUD-1", "trace-1");
    }

    @Test
    void recordLoginSuccessPersistsDurableAuditEvent() {
        service.recordLoginSuccess("10000001", "AUD-2", "trace-2");

        verify(auditRecorder).recordSecurityLoginSuccess("10000001", "AUD-2", "trace-2");
    }

    @Test
    void recordRouteAccessDeniedPersistsWithReasonCode() {
        service.recordRouteAccessDenied(
                "10000003",
                "route.audit-console",
                SecurityAuditSummaryService.REASON_ROUTE_NOT_VISIBLE,
                "AUD-3",
                "trace-3"
        );

        verify(auditRecorder).recordSecurityRouteAccessDenied(
                "10000003",
                "route.audit-console",
                SecurityAuditSummaryService.REASON_ROUTE_NOT_VISIBLE,
                "AUD-3",
                "trace-3"
        );
    }

    @Test
    void recordDocumentDownloadPersistsGrantWithTemplateScope() {
        UUID templateId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        service.recordDocumentDownload(
                "CRED-1",
                "svc-call",
                "DOC-12345678",
                "TPL-001",
                templateId,
                "RETAIL",
                "AUD-4",
                "trace-4"
        );

        verify(auditRecorder).recordSecurityDocumentDownload(
                "CRED-1",
                "svc-call",
                "DOC-12345678",
                "TPL-001",
                templateId,
                "RETAIL",
                "AUD-4",
                "trace-4"
        );
    }

    @Test
    void recordDocumentDownloadDeniedPersistsDenial() {
        UUID templateId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        service.recordDocumentDownloadDenied(
                "CRED-1",
                "svc-call",
                "DOC-12345678",
                "TPL-001",
                templateId,
                "RETAIL",
                SecurityAuditSummaryService.REASON_DOWNLOAD_ACCESS_DENIED,
                "AUD-5",
                "trace-5"
        );

        verify(auditRecorder).recordSecurityDocumentDownloadDenied(
                "CRED-1",
                "svc-call",
                "DOC-12345678",
                "TPL-001",
                templateId,
                "RETAIL",
                SecurityAuditSummaryService.REASON_DOWNLOAD_ACCESS_DENIED,
                "AUD-5",
                "trace-5"
        );
    }

    @Test
    void persistenceFailureDoesNotPropagateOnLoginSuccess() {
        doThrow(new RuntimeException("db down"))
                .when(auditRecorder)
                .recordSecurityLoginSuccess(anyString(), anyString(), anyString());

        assertThatCode(() -> service.recordLoginSuccess("10000001", "AUD-6", "trace-6"))
                .doesNotThrowAnyException();
    }

    @Test
    void persistenceFailureDoesNotPropagateOnLoginFailure() {
        doThrow(new RuntimeException("db down"))
                .when(auditRecorder)
                .recordSecurityLoginFailure(anyString(), anyString(), anyString());

        assertThatCode(() -> service.recordLoginFailure("10000001", "AUD-7", "trace-7"))
                .doesNotThrowAnyException();
    }

    @Test
    void persistenceFailureDoesNotPropagateOnRouteDenied() {
        doThrow(new RuntimeException("db down"))
                .when(auditRecorder)
                .recordSecurityRouteAccessDenied(
                        anyString(), anyString(), anyString(), anyString(), anyString());

        assertThatCode(() -> service.recordRouteAccessDenied(
                "10000003",
                "route.audit-console",
                SecurityAuditSummaryService.REASON_ACCESS_DENIED,
                "AUD-8",
                "trace-8"
        )).doesNotThrowAnyException();
    }

    @Test
    void persistenceFailureDoesNotPropagateOnDownloadDenied() {
        doThrow(new RuntimeException("db down"))
                .when(auditRecorder)
                .recordSecurityDocumentDownloadDenied(
                        anyString(),
                        anyString(),
                        anyString(),
                        anyString(),
                        any(),
                        any(),
                        anyString(),
                        anyString(),
                        anyString()
                );

        assertThatCode(() -> service.recordDocumentDownloadDenied(
                "CRED-1",
                "svc-call",
                "DOC-1",
                "TPL-001",
                null,
                null,
                SecurityAuditSummaryService.REASON_DOWNLOAD_NOT_AVAILABLE,
                "AUD-9",
                "trace-9"
        )).doesNotThrowAnyException();

        verify(auditRecorder).recordSecurityDocumentDownloadDenied(
                eq("CRED-1"),
                eq("svc-call"),
                eq("DOC-1"),
                eq("TPL-001"),
                isNull(),
                isNull(),
                eq(SecurityAuditSummaryService.REASON_DOWNLOAD_NOT_AVAILABLE),
                eq("AUD-9"),
                eq("trace-9")
        );
    }
}
