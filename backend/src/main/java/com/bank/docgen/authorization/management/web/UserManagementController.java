package com.bank.docgen.authorization.management.web;

import com.bank.docgen.authorization.management.api.CreateUserRequest;
import com.bank.docgen.authorization.management.api.ManagementUserView;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.api.ResetPasswordRequest;
import com.bank.docgen.authorization.management.api.UpdateUserRequest;
import com.bank.docgen.authorization.management.service.UserManagementService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/users")
public class UserManagementController {

    private final UserManagementService userManagementService;
    private final TraceIdProvider traceIdProvider;

    public UserManagementController(
            UserManagementService userManagementService,
            TraceIdProvider traceIdProvider
    ) {
        this.userManagementService = userManagementService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<PageView<ManagementUserView>> list(
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, userManagementService.list(session, group, role, page, size));
    }

    @GetMapping("/{id}")
    public SuccessEnvelope<ManagementUserView> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, userManagementService.get(id, session));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessEnvelope<ManagementUserView> create(
            @Valid @RequestBody CreateUserRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, userManagementService.create(body, session));
    }

    @PutMapping("/{id}")
    public SuccessEnvelope<ManagementUserView> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, userManagementService.update(id, body, session));
    }

    @PostMapping("/{id}/disable")
    public SuccessEnvelope<ManagementUserView> disable(
            @PathVariable UUID id,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, userManagementService.disable(id, session));
    }

    @PostMapping("/{id}/enable")
    public SuccessEnvelope<ManagementUserView> enable(
            @PathVariable UUID id,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, userManagementService.enable(id, session));
    }

    @PostMapping("/{id}/reset-password")
    public SuccessEnvelope<ManagementUserView> resetPassword(
            @PathVariable UUID id,
            @Valid @RequestBody ResetPasswordRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, userManagementService.resetPassword(id, body, session));
    }

    @DeleteMapping("/{id}")
    public SuccessEnvelope<ManagementUserView> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, userManagementService.delete(id, session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
