package com.bank.docgen.collaboration.web;

import com.bank.docgen.collaboration.api.CollaborationTimeoutConfigView;
import com.bank.docgen.collaboration.api.UpsertCollaborationTimeoutConfigRequest;
import com.bank.docgen.collaboration.service.CollaborationTimeoutConfigService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/collaboration-timeout-config")
public class CollaborationTimeoutConfigController {

    private final CollaborationTimeoutConfigService configService;
    private final TraceIdProvider traceIdProvider;

    public CollaborationTimeoutConfigController(
            CollaborationTimeoutConfigService configService,
            TraceIdProvider traceIdProvider
    ) {
        this.configService = configService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<CollaborationTimeoutConfigView> get(
            @RequestParam(required = false) String groupCode,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, configService.resolve(groupCode, session));
    }

    @PutMapping
    public SuccessEnvelope<CollaborationTimeoutConfigView> upsert(
            @Valid @RequestBody UpsertCollaborationTimeoutConfigRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, configService.upsert(body, session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
