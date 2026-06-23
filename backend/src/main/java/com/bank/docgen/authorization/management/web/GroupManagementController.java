package com.bank.docgen.authorization.management.web;

import com.bank.docgen.authorization.management.api.BusinessGroupView;
import com.bank.docgen.authorization.management.api.CreateGroupRequest;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.api.UpdateGroupRequest;
import com.bank.docgen.authorization.management.service.BusinessGroupService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/management/v1/groups")
public class GroupManagementController {

    private final BusinessGroupService businessGroupService;
    private final TraceIdProvider traceIdProvider;

    public GroupManagementController(
            BusinessGroupService businessGroupService,
            TraceIdProvider traceIdProvider
    ) {
        this.businessGroupService = businessGroupService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<PageView<BusinessGroupView>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, businessGroupService.list(session, page, size));
    }

    @GetMapping("/{id}")
    public SuccessEnvelope<BusinessGroupView> get(
            @PathVariable UUID id,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, businessGroupService.get(id, session));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessEnvelope<BusinessGroupView> create(
            @Valid @RequestBody CreateGroupRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, businessGroupService.create(body, session));
    }

    @PutMapping("/{id}")
    public SuccessEnvelope<BusinessGroupView> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGroupRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, businessGroupService.updateDisplayName(id, body, session));
    }

    @PostMapping("/{id}/disable")
    public SuccessEnvelope<BusinessGroupView> disable(
            @PathVariable UUID id,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, businessGroupService.disable(id, session));
    }

    @PostMapping("/{id}/enable")
    public SuccessEnvelope<BusinessGroupView> enable(
            @PathVariable UUID id,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, businessGroupService.enable(id, session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
