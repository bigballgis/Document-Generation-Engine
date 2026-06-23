package com.bank.docgen.audit.web;

import com.bank.docgen.audit.api.LifecycleAuditExportResult;
import com.bank.docgen.audit.api.LifecycleAuditQueryResult;
import com.bank.docgen.audit.api.ManagementAuditExportResult;
import com.bank.docgen.audit.api.ManagementAuditQueryResult;
import com.bank.docgen.audit.domain.AuditReadActorRole;
import com.bank.docgen.audit.service.AuditQueryService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/admin/audit")
public class AuditController {

    private final AuditQueryService auditQueryService;
    private final TraceIdProvider traceIdProvider;

    public AuditController(AuditQueryService auditQueryService, TraceIdProvider traceIdProvider) {
        this.auditQueryService = auditQueryService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping("/management-events")
    public SuccessEnvelope<ManagementAuditQueryResult> getManagementEvents(
            @RequestParam AuditReadActorRole actorRole,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) UUID credentialId,
            @RequestParam(required = false) Instant eventAtFrom,
            @RequestParam(required = false) Instant eventAtTo,
            @RequestParam(required = false) String groupScope,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, auditQueryService.queryManagementEvents(
                session,
                actorRole,
                templateId,
                eventType,
                credentialId,
                eventAtFrom,
                eventAtTo,
                groupScope
        ));
    }

    @GetMapping("/management-events/export")
    public SuccessEnvelope<ManagementAuditExportResult> exportManagementEvents(
            @RequestParam AuditReadActorRole actorRole,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) UUID credentialId,
            @RequestParam(required = false) Instant eventAtFrom,
            @RequestParam(required = false) Instant eventAtTo,
            @RequestParam(required = false) String groupScope,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, auditQueryService.exportManagementEvents(
                session,
                actorRole,
                templateId,
                eventType,
                credentialId,
                eventAtFrom,
                eventAtTo,
                groupScope
        ));
    }

    @GetMapping("/lifecycle-events")
    public SuccessEnvelope<LifecycleAuditQueryResult> getLifecycleEvents(
            @RequestParam AuditReadActorRole actorRole,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Instant eventAtFrom,
            @RequestParam(required = false) Instant eventAtTo,
            @RequestParam(required = false) String groupScope,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, auditQueryService.queryLifecycleEvents(
                session,
                actorRole,
                templateId,
                eventType,
                eventAtFrom,
                eventAtTo,
                groupScope
        ));
    }

    @GetMapping("/lifecycle-events/export")
    public SuccessEnvelope<LifecycleAuditExportResult> exportLifecycleEvents(
            @RequestParam AuditReadActorRole actorRole,
            @RequestParam(required = false) UUID templateId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Instant eventAtFrom,
            @RequestParam(required = false) Instant eventAtTo,
            @RequestParam(required = false) String groupScope,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, auditQueryService.exportLifecycleEvents(
                session,
                actorRole,
                templateId,
                eventType,
                eventAtFrom,
                eventAtTo,
                groupScope
        ));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
