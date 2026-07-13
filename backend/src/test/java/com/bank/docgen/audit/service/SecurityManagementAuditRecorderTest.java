package com.bank.docgen.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import com.bank.docgen.authorization.management.service.SecurityAuditSummaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * BDD-LRP-D7-001…006, 010 — durable SECURITY_* rows on management_audit_event.
 */
@ExtendWith(MockitoExtension.class)
class SecurityManagementAuditRecorderTest {

    @Mock
    private ManagementAuditEventRepository repository;

    private SecurityManagementAuditRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new SecurityManagementAuditRecorder(repository, new ObjectMapper());
        lenient().when(repository.save(any(ManagementAuditEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void recordLoginFailureWritesPlatformLevelRowWithTrace() {
        recorder.recordSecurityLoginFailure("10000001", "AUD-1", "trace-1");

        ManagementAuditEventEntity saved = captureSaved();
        assertThat(saved.getEventType()).isEqualTo(SecurityManagementAuditRecorder.SECURITY_LOGIN_FAILURE);
        assertThat(saved.getActorUsername()).isEqualTo("10000001");
        assertThat(saved.getGroupCode()).isNull();
        assertThat(saved.getTemplateId()).isNull();
        assertThat(saved.getStatusSummary()).containsIgnoringCase("failure");
        assertThat(saved.getWarningCodesJson()).contains("trace-1").contains("AUD-1");
        assertThat(saved.getStatusSummary()).doesNotContain("password");
    }

    @Test
    void recordRouteAccessDeniedIncludesRouteKeyAndReason() {
        recorder.recordSecurityRouteAccessDenied(
                "10000003",
                "route.audit-console",
                SecurityAuditSummaryService.REASON_ROUTE_NOT_VISIBLE,
                "AUD-2",
                "trace-2"
        );

        ManagementAuditEventEntity saved = captureSaved();
        assertThat(saved.getEventType())
                .isEqualTo(SecurityManagementAuditRecorder.SECURITY_ROUTE_ACCESS_DENIED);
        assertThat(saved.getStatusSummary()).contains("route.audit-console");
        assertThat(saved.getWarningCodesJson())
                .contains("trace-2")
                .contains("AUD-2")
                .contains(SecurityAuditSummaryService.REASON_ROUTE_NOT_VISIBLE);
        assertThat(saved.getGroupCode()).isNull();
    }

    @Test
    void recordDocumentDownloadWritesTemplateScopeWithoutFullUrl() {
        UUID templateId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        recorder.recordSecurityDocumentDownload(
                "CRED-1",
                "svc-caller",
                "DOC-12345678",
                "TPL-001",
                templateId,
                "RETAIL",
                "AUD-3",
                "trace-3"
        );

        ManagementAuditEventEntity saved = captureSaved();
        assertThat(saved.getEventType())
                .isEqualTo(SecurityManagementAuditRecorder.SECURITY_DOCUMENT_DOWNLOAD);
        assertThat(saved.getTemplateId()).isEqualTo(templateId);
        assertThat(saved.getGroupCode()).isEqualTo("RETAIL");
        assertThat(saved.getCredentialFingerprint()).isEqualTo("fp-CRED-1");
        assertThat(saved.getActorUsername()).isEqualTo("svc-call");
        assertThat(saved.getStatusSummary()).contains("DOC-12345678");
        assertThat(saved.getStatusSummary()).doesNotContain("http");
        assertThat(saved.getWarningCodesJson()).contains("trace-3").contains("AUD-3");
    }

    @Test
    void recordDocumentDownloadDeniedWritesReasonCode() {
        UUID templateId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        recorder.recordSecurityDocumentDownloadDenied(
                "CRED-1",
                "svc-caller",
                "DOC-12345678",
                "TPL-001",
                templateId,
                "RETAIL",
                SecurityAuditSummaryService.REASON_DOWNLOAD_EXPIRED,
                "AUD-4",
                "trace-4"
        );

        ManagementAuditEventEntity saved = captureSaved();
        assertThat(saved.getEventType())
                .isEqualTo(SecurityManagementAuditRecorder.SECURITY_DOCUMENT_DOWNLOAD_DENIED);
        assertThat(saved.getWarningCodesJson())
                .contains(SecurityAuditSummaryService.REASON_DOWNLOAD_EXPIRED);
        assertThat(saved.getGroupCode()).isEqualTo("RETAIL");
        assertThat(saved.getTemplateId()).isEqualTo(templateId);
    }

    @Test
    void securityWriteMethodsUseRequiresNewPropagation() throws Exception {
        for (String methodName : new String[]{
                "recordSecurityLoginSuccess",
                "recordSecurityLoginFailure",
                "recordSecurityLogout",
                "recordSecurityRouteAccessDenied",
                "recordSecurityDocumentDownload",
                "recordSecurityDocumentDownloadDenied"
        }) {
            Method method = findPublicMethod(methodName);
            Transactional transactional = method.getAnnotation(Transactional.class);
            assertThat(transactional)
                    .as("%s must be @Transactional(REQUIRES_NEW)", methodName)
                    .isNotNull();
            assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        }
    }

    private ManagementAuditEventEntity captureSaved() {
        ArgumentCaptor<ManagementAuditEventEntity> captor =
                ArgumentCaptor.forClass(ManagementAuditEventEntity.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private static Method findPublicMethod(String name) {
        for (Method method : SecurityManagementAuditRecorder.class.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getAnnotation(Transactional.class) != null) {
                return method;
            }
        }
        throw new AssertionError("No @Transactional method named " + name);
    }
}
