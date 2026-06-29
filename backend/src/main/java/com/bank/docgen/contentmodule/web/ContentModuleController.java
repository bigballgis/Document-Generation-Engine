package com.bank.docgen.contentmodule.web;

import com.bank.docgen.contentmodule.api.ContentModuleDetailView;
import com.bank.docgen.contentmodule.api.ContentModuleLifecycleOperationApplyRequest;
import com.bank.docgen.contentmodule.api.ContentModuleLifecycleOperationResultView;
import com.bank.docgen.contentmodule.api.ContentModuleReviewTransitionRequest;
import com.bank.docgen.contentmodule.api.ContentModuleReviewTransitionResultView;
import com.bank.docgen.contentmodule.api.ContentModuleSummaryView;
import com.bank.docgen.contentmodule.api.CreateContentModuleRequest;
import com.bank.docgen.contentmodule.api.CreateContentModuleVersionRequest;
import com.bank.docgen.contentmodule.api.UpdateContentModuleVersionRequest;
import com.bank.docgen.contentmodule.api.ContentModuleLifecycleImpactSummaryView;
import com.bank.docgen.contentmodule.service.ContentModuleLifecycleImpactService;
import com.bank.docgen.contentmodule.service.ContentModuleLifecycleService;
import com.bank.docgen.contentmodule.service.ContentModuleReviewService;
import com.bank.docgen.contentmodule.service.ContentModuleService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/content-modules")
public class ContentModuleController {

    private final ContentModuleService contentModuleService;
    private final ContentModuleReviewService reviewService;
    private final ContentModuleLifecycleService lifecycleService;
    private final ContentModuleLifecycleImpactService lifecycleImpactService;
    private final TraceIdProvider traceIdProvider;

    public ContentModuleController(
            ContentModuleService contentModuleService,
            ContentModuleReviewService reviewService,
            ContentModuleLifecycleService lifecycleService,
            ContentModuleLifecycleImpactService lifecycleImpactService,
            TraceIdProvider traceIdProvider
    ) {
        this.contentModuleService = contentModuleService;
        this.reviewService = reviewService;
        this.lifecycleService = lifecycleService;
        this.lifecycleImpactService = lifecycleImpactService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<List<ContentModuleSummaryView>> list(
            @RequestParam String groupCode,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, contentModuleService.list(groupCode, session));
    }

    @GetMapping("/{moduleId}")
    public SuccessEnvelope<ContentModuleDetailView> get(
            @PathVariable String moduleId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, contentModuleService.get(moduleId, session));
    }

    @PostMapping
    public SuccessEnvelope<ContentModuleDetailView> create(
            @Valid @RequestBody CreateContentModuleRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, contentModuleService.create(body, session));
    }

    @PostMapping("/{moduleId}/versions")
    public SuccessEnvelope<ContentModuleDetailView> createVersion(
            @PathVariable String moduleId,
            @Valid @RequestBody CreateContentModuleVersionRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, contentModuleService.createVersion(moduleId, body, session));
    }

    @PutMapping("/{moduleId}/versions/{semanticVersion}")
    public SuccessEnvelope<ContentModuleDetailView> updateDraftVersion(
            @PathVariable String moduleId,
            @PathVariable String semanticVersion,
            @Valid @RequestBody UpdateContentModuleVersionRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, contentModuleService.updateDraftVersion(moduleId, semanticVersion, body, session));
    }

    @PostMapping("/{moduleId}/review/transition")
    public SuccessEnvelope<ContentModuleReviewTransitionResultView> reviewTransition(
            @PathVariable String moduleId,
            @Valid @RequestBody ContentModuleReviewTransitionRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, reviewService.transition(moduleId, body, session));
    }

    @PostMapping("/{moduleId}/lifecycle/operation/apply")
    public SuccessEnvelope<ContentModuleLifecycleOperationResultView> lifecycleApply(
            @PathVariable String moduleId,
            @Valid @RequestBody ContentModuleLifecycleOperationApplyRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, lifecycleService.apply(moduleId, body, session));
    }

    @GetMapping("/{moduleId}/lifecycle/impact/preview")
    public SuccessEnvelope<ContentModuleLifecycleImpactSummaryView> lifecycleImpactPreview(
            @PathVariable String moduleId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, lifecycleImpactService.previewImpact(moduleId, session));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
