package com.bank.docgen.authorization.management.service;

import com.bank.docgen.audit.service.SecurityManagementAuditRecorder;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SecurityAuditSummaryService {

    public static final String REASON_ROUTE_NOT_VISIBLE = "ROUTE_NOT_VISIBLE";
    public static final String REASON_ACCESS_DENIED = "ACCESS_DENIED";
    public static final String REASON_DOWNLOAD_ACCESS_DENIED = "DOWNLOAD_ACCESS_DENIED";
    public static final String REASON_DOWNLOAD_EXPIRED = "DOWNLOAD_EXPIRED";
    public static final String REASON_DOWNLOAD_NOT_AVAILABLE = "DOWNLOAD_NOT_AVAILABLE";

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAuditSummaryService.class);

    private final SecurityManagementAuditRecorder auditRecorder;

    public SecurityAuditSummaryService(SecurityManagementAuditRecorder auditRecorder) {
        this.auditRecorder = auditRecorder;
    }

    public void recordLoginSuccess(String username, String auditId, String traceId) {
        LOGGER.info(
                "security.audit.login.success username={} auditId={} traceId={}",
                username,
                auditId,
                traceId
        );
        persistSafely(
                SecurityManagementAuditRecorder.SECURITY_LOGIN_SUCCESS,
                traceId,
                () -> auditRecorder.recordSecurityLoginSuccess(username, auditId, traceId)
        );
    }

    public void recordLoginFailure(String username, String auditId, String traceId) {
        LOGGER.warn(
                "security.audit.login.failure username={} auditId={} traceId={}",
                username,
                auditId,
                traceId
        );
        persistSafely(
                SecurityManagementAuditRecorder.SECURITY_LOGIN_FAILURE,
                traceId,
                () -> auditRecorder.recordSecurityLoginFailure(username, auditId, traceId)
        );
    }

    public void recordLogout(String username, String auditId, String traceId) {
        LOGGER.info(
                "security.audit.logout username={} auditId={} traceId={}",
                username,
                auditId,
                traceId
        );
        persistSafely(
                SecurityManagementAuditRecorder.SECURITY_LOGOUT,
                traceId,
                () -> auditRecorder.recordSecurityLogout(username, auditId, traceId)
        );
    }

    public void recordSessionRenewal(String username, String auditId, String traceId) {
        LOGGER.info(
                "security.audit.session.renewal username={} auditId={} traceId={}",
                username,
                auditId,
                traceId
        );
    }

    public void recordSessionRenewalDenied(String username, String reason, String auditId, String traceId) {
        LOGGER.warn(
                "security.audit.session.renewal.denied username={} reason={} auditId={} traceId={}",
                username,
                reason,
                auditId,
                traceId
        );
    }

    public void recordRouteAccessDenied(
            String username,
            String routeKey,
            String reasonCode,
            String auditId,
            String traceId
    ) {
        LOGGER.warn(
                "security.audit.route.denied username={} routeKey={} auditId={} traceId={}",
                username,
                routeKey,
                auditId,
                traceId
        );
        persistSafely(
                SecurityManagementAuditRecorder.SECURITY_ROUTE_ACCESS_DENIED,
                traceId,
                () -> auditRecorder.recordSecurityRouteAccessDenied(
                        username, routeKey, reasonCode, auditId, traceId)
        );
    }

    public void recordDocumentDownload(
            String credentialExternalId,
            String accessAccount,
            String documentId,
            String templateExternalId,
            UUID templateId,
            String groupCode,
            String auditId,
            String traceId
    ) {
        LOGGER.info(
                "security.audit.download.success credentialId={} accessAccount={} documentId={} templateId={} auditId={} traceId={}",
                credentialExternalId,
                accessAccount,
                documentId,
                templateExternalId,
                auditId,
                traceId
        );
        persistSafely(
                SecurityManagementAuditRecorder.SECURITY_DOCUMENT_DOWNLOAD,
                traceId,
                () -> auditRecorder.recordSecurityDocumentDownload(
                        credentialExternalId,
                        accessAccount,
                        documentId,
                        templateExternalId,
                        templateId,
                        groupCode,
                        auditId,
                        traceId
                )
        );
    }

    public void recordDocumentDownloadDenied(
            String credentialExternalId,
            String accessAccount,
            String documentId,
            String templateExternalId,
            UUID templateId,
            String groupCode,
            String reasonCode,
            String auditId,
            String traceId
    ) {
        LOGGER.warn(
                "security.audit.download.denied credentialId={} accessAccount={} documentId={} templateId={} reason={} auditId={} traceId={}",
                credentialExternalId,
                accessAccount,
                documentId,
                templateExternalId,
                reasonCode,
                auditId,
                traceId
        );
        persistSafely(
                SecurityManagementAuditRecorder.SECURITY_DOCUMENT_DOWNLOAD_DENIED,
                traceId,
                () -> auditRecorder.recordSecurityDocumentDownloadDenied(
                        credentialExternalId,
                        accessAccount,
                        documentId,
                        templateExternalId,
                        templateId,
                        groupCode,
                        reasonCode,
                        auditId,
                        traceId
                )
        );
    }

    private void persistSafely(String eventType, String traceId, Runnable persist) {
        try {
            persist.run();
        } catch (RuntimeException ex) {
            LOGGER.warn(
                    "security.audit.persist.failed eventType={} traceId={} error={}",
                    eventType,
                    traceId,
                    ex.getClass().getName()
            );
        }
    }
}
