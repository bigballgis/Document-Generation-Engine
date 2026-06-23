package com.bank.docgen.authorization.management.web;

import com.bank.docgen.authorization.management.api.LoginRequest;
import com.bank.docgen.authorization.management.api.LoginResult;
import com.bank.docgen.authorization.management.api.ManagementSessionView;
import com.bank.docgen.authorization.management.service.ManagementAuthService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/auth")
public class ManagementAuthController {

    private final ManagementAuthService managementAuthService;
    private final TraceIdProvider traceIdProvider;

    public ManagementAuthController(
            ManagementAuthService managementAuthService,
            TraceIdProvider traceIdProvider
    ) {
        this.managementAuthService = managementAuthService;
        this.traceIdProvider = traceIdProvider;
    }

    @PostMapping("/login")
    public SuccessEnvelope<LoginResult> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String traceId = traceIdProvider.currentOrNew(httpRequest.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        ManagementAuthService.LoginSession loginSession = managementAuthService.authenticate(
                request.username(),
                request.password(),
                auditId,
                traceId
        );
        LoginResult result = new LoginResult(loginSession.accessToken(), "Bearer", loginSession.session());
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest httpRequest
    ) {
        String traceId = traceIdProvider.currentOrNew(httpRequest.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        managementAuthService.logout(session, auditId, traceId);
    }

    @GetMapping("/session")
    public SuccessEnvelope<ManagementSessionView> session(
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest httpRequest
    ) {
        String traceId = traceIdProvider.currentOrNew(httpRequest.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        ManagementSessionView current = managementAuthService.currentSession(session);
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), current);
    }
}
