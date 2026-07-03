package com.bank.docgen.rendering.web;

import com.bank.docgen.rendering.api.AsyncBatchStartResponse;
import com.bank.docgen.rendering.api.BatchTestRunSummaryView;
import com.bank.docgen.rendering.api.SubmitTestEligibilityView;
import com.bank.docgen.rendering.service.AsyncBatchTestOrchestrator;
import com.bank.docgen.rendering.service.BatchTestHistoryService;
import com.bank.docgen.rendering.service.BatchTestRunNotFoundException;
import com.bank.docgen.rendering.service.SubmitTestEligibilityService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/management/v1/templates/{templateId}/batch-tests")
public class BatchTestController {

    private final AsyncBatchTestOrchestrator batchTestOrchestrator;
    private final BatchTestHistoryService batchTestHistoryService;
    private final SubmitTestEligibilityService eligibilityService;
    private final TraceIdProvider traceIdProvider;

    public BatchTestController(
            AsyncBatchTestOrchestrator batchTestOrchestrator,
            BatchTestHistoryService batchTestHistoryService,
            SubmitTestEligibilityService eligibilityService,
            TraceIdProvider traceIdProvider
    ) {
        this.batchTestOrchestrator = batchTestOrchestrator;
        this.batchTestHistoryService = batchTestHistoryService;
        this.eligibilityService = eligibilityService;
        this.traceIdProvider = traceIdProvider;
    }

    @PostMapping("/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SuccessEnvelope<AsyncBatchStartResponse> startBatchRun(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        String baseUrl = buildBaseUrl(request, templateId);
        AsyncBatchStartResponse response = batchTestOrchestrator.startBatchRun(templateId, session, baseUrl);
        return envelope(request, response);
    }

    @GetMapping(value = "/{runId}/progress-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBatchProgress(
            @PathVariable UUID templateId,
            @PathVariable UUID runId,
            @AuthenticationPrincipal ManagementSessionClaims session
    ) {
        return batchTestOrchestrator.streamProgress(templateId, runId, session);
    }

    @GetMapping
    public SuccessEnvelope<List<BatchTestRunSummaryView>> listRecentRuns(
            @PathVariable UUID templateId,
            @RequestParam(defaultValue = "5") int limit,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        List<BatchTestRunSummaryView> runs = batchTestHistoryService.listRecentRuns(templateId, limit, session);
        return envelope(request, runs);
    }

    @GetMapping("/submit-eligibility")
    public SuccessEnvelope<SubmitTestEligibilityView> getSubmitEligibility(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, eligibilityService.evaluate(templateId, session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }

    private String buildBaseUrl(HttpServletRequest request, UUID templateId) {
        return request.getScheme() + "://" + request.getServerName()
                + ":" + request.getServerPort()
                + "/api/management/v1/templates/" + templateId + "/batch-tests";
    }
}
