package com.bank.docgen.collaboration.web;

import com.bank.docgen.collaboration.api.CollaborationNotificationItemView;
import com.bank.docgen.collaboration.api.CollaborationNotificationUnreadCountView;
import com.bank.docgen.collaboration.service.CollaborationNotificationService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/collaboration-notifications")
public class CollaborationNotificationController {

    private final CollaborationNotificationService notificationService;
    private final TraceIdProvider traceIdProvider;

    public CollaborationNotificationController(
            CollaborationNotificationService notificationService,
            TraceIdProvider traceIdProvider
    ) {
        this.notificationService = notificationService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping("/unread-count")
    public SuccessEnvelope<CollaborationNotificationUnreadCountView> unreadCount(
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, notificationService.unreadCount(session));
    }

    @GetMapping
    public SuccessEnvelope<List<CollaborationNotificationItemView>> list(
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, notificationService.list(session));
    }

    @PostMapping("/{workItemId}/read")
    public SuccessEnvelope<CollaborationNotificationUnreadCountView> markRead(
            @PathVariable UUID workItemId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, notificationService.markRead(session, workItemId));
    }

    @PostMapping("/read-all")
    public SuccessEnvelope<CollaborationNotificationUnreadCountView> markAllRead(
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, notificationService.markAllRead(session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
