package com.bank.docgen.audit.service;

import com.bank.docgen.audit.persistence.ManagementAuditEventEntity;
import com.bank.docgen.audit.persistence.ManagementAuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityManagementAuditRecorder {

    public static final String SECURITY_LOGIN_SUCCESS = "SECURITY_LOGIN_SUCCESS";
    public static final String SECURITY_LOGIN_FAILURE = "SECURITY_LOGIN_FAILURE";
    public static final String SECURITY_LOGOUT = "SECURITY_LOGOUT";
    public static final String SECURITY_ROUTE_ACCESS_DENIED = "SECURITY_ROUTE_ACCESS_DENIED";
    public static final String SECURITY_DOCUMENT_DOWNLOAD = "SECURITY_DOCUMENT_DOWNLOAD";

    private static final int STATUS_SUMMARY_MAX = 512;

    private final ManagementAuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public SecurityManagementAuditRecorder(
            ManagementAuditEventRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordSecurityLoginSuccess(String username, String auditId, String traceId) {
        recordSecurityEvent(SECURITY_LOGIN_SUCCESS, username, auditId, traceId, "Login success");
    }

    @Transactional
    public void recordSecurityLoginFailure(String username, String auditId, String traceId) {
        recordSecurityEvent(SECURITY_LOGIN_FAILURE, username, auditId, traceId, "Login failure");
    }

    @Transactional
    public void recordSecurityLogout(String username, String auditId, String traceId) {
        recordSecurityEvent(SECURITY_LOGOUT, username, auditId, traceId, "Logout");
    }

    @Transactional
    public void recordSecurityRouteAccessDenied(
            String username,
            String routeKey,
            String auditId,
            String traceId
    ) {
        recordSecurityEvent(
                SECURITY_ROUTE_ACCESS_DENIED,
                username,
                auditId,
                traceId,
                "Route access denied: " + routeKey
        );
    }

    @Transactional
    public void recordSecurityDocumentDownload(
            String credentialExternalId,
            String accessAccount,
            String documentId,
            String templateExternalId,
            String auditId,
            String traceId
    ) {
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                SECURITY_DOCUMENT_DOWNLOAD,
                null,
                null,
                null,
                null,
                null,
                writeJson(List.of(documentId, templateExternalId)),
                false,
                null,
                sanitizeActorUsername(accessAccount),
                "Runtime download",
                fingerprint(credentialExternalId),
                truncate("Document download: " + documentId),
                writeJson(List.of(traceId, auditId))
        ));
    }

    private void recordSecurityEvent(
            String eventType,
            String username,
            String auditId,
            String traceId,
            String statusSummary
    ) {
        repository.save(new ManagementAuditEventEntity(
                UUID.randomUUID(),
                Instant.now(),
                eventType,
                null,
                null,
                null,
                null,
                null,
                writeJson(List.of()),
                false,
                null,
                sanitizeActorUsername(username),
                "Security audit",
                null,
                truncate(statusSummary),
                writeJson(List.of(traceId, auditId))
        ));
    }

    private String sanitizeActorUsername(String username) {
        if (username == null || username.isBlank()) {
            return "00000000";
        }
        return username.length() <= 8 ? username : username.substring(0, 8);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= STATUS_SUMMARY_MAX ? value : value.substring(0, STATUS_SUMMARY_MAX);
    }

    private String fingerprint(String externalId) {
        return externalId == null ? null : "fp-" + externalId;
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            return "[]";
        }
    }
}
