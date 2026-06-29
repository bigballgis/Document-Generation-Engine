package com.bank.docgen.collaboration.web;

import com.bank.docgen.collaboration.api.CollaborationWorkItemSummaryView;
import com.bank.docgen.collaboration.domain.CollaborationWorkItemQueue;
import com.bank.docgen.collaboration.service.CollaborationWorkItemService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/collaboration-work-items")
public class CollaborationWorkItemController {

    private final CollaborationWorkItemService workItemService;
    private final TraceIdProvider traceIdProvider;

    public CollaborationWorkItemController(
            CollaborationWorkItemService workItemService,
            TraceIdProvider traceIdProvider
    ) {
        this.workItemService = workItemService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<List<CollaborationWorkItemSummaryView>> listQueue(
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) CollaborationWorkItemQueue queue,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, workItemService.listQueue(session, groupCode, queue));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
