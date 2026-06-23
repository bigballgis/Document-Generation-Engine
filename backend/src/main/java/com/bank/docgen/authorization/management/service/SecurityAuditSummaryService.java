package com.bank.docgen.authorization.management.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SecurityAuditSummaryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAuditSummaryService.class);

    public void recordLoginSuccess(String username, String auditId, String traceId) {
        LOGGER.info(
                "security.audit.login.success username={} auditId={} traceId={}",
                username,
                auditId,
                traceId
        );
    }

    public void recordLoginFailure(String username, String auditId, String traceId) {
        LOGGER.warn(
                "security.audit.login.failure username={} auditId={} traceId={}",
                username,
                auditId,
                traceId
        );
    }

    public void recordLogout(String username, String auditId, String traceId) {
        LOGGER.info(
                "security.audit.logout username={} auditId={} traceId={}",
                username,
                auditId,
                traceId
        );
    }

    public void recordRouteAccessDenied(String username, String routeKey, String auditId, String traceId) {
        LOGGER.warn(
                "security.audit.route.denied username={} routeKey={} auditId={} traceId={}",
                username,
                routeKey,
                auditId,
                traceId
        );
    }

    public void recordDocumentDownload(
            String credentialExternalId,
            String accessAccount,
            String documentId,
            String templateExternalId,
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
    }
}
