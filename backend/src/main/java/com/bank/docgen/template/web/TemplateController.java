package com.bank.docgen.template.web;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ChangeDiffView;
import com.bank.docgen.template.api.CoverageSummaryView;
import com.bank.docgen.template.api.CreateTemplateRequest;
import com.bank.docgen.template.api.LifecycleGovernanceRequest;
import com.bank.docgen.template.api.PublishGateChecklistView;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.api.TemplateReleaseVersionView;
import com.bank.docgen.template.api.TemplateSummaryView;
import com.bank.docgen.template.api.UpdateTemplateRequest;
import com.bank.docgen.template.domain.PublishGatePhase;
import com.bank.docgen.template.service.ChangeDiffService;
import com.bank.docgen.template.service.CoverageComputationService;
import com.bank.docgen.template.service.PublishGateService;
import com.bank.docgen.template.service.TemplateDeleteService;
import com.bank.docgen.template.service.TemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/templates")
public class TemplateController {

    private final TemplateService templateService;
    private final TemplateDeleteService templateDeleteService;
    private final CoverageComputationService coverageComputationService;
    private final ChangeDiffService changeDiffService;
    private final PublishGateService publishGateService;
    private final TraceIdProvider traceIdProvider;

    public TemplateController(
            TemplateService templateService,
            TemplateDeleteService templateDeleteService,
            CoverageComputationService coverageComputationService,
            ChangeDiffService changeDiffService,
            PublishGateService publishGateService,
            TraceIdProvider traceIdProvider
    ) {
        this.templateService = templateService;
        this.templateDeleteService = templateDeleteService;
        this.coverageComputationService = coverageComputationService;
        this.changeDiffService = changeDiffService;
        this.publishGateService = publishGateService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<PageView<TemplateSummaryView>> list(
            @AuthenticationPrincipal ManagementSessionClaims session,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) String lifecycleStatus,
            @RequestParam(required = false) String approvalSubState,
            @RequestParam(required = false) String sort,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.list(
                session, page, size, search, groupCode, lifecycleStatus, approvalSubState, sort));
    }

    @GetMapping("/{templateId}")
    public SuccessEnvelope<TemplateDetailView> get(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.get(templateId, session));
    }

    @GetMapping("/{templateId}/coverage")
    public SuccessEnvelope<CoverageSummaryView> coverage(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, coverageComputationService.compute(templateId, session));
    }

    @GetMapping("/{templateId}/change-diff")
    public SuccessEnvelope<ChangeDiffView> changeDiff(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, changeDiffService.compute(templateId, session));
    }

    @GetMapping("/{templateId}/publish-gate")
    public SuccessEnvelope<PublishGateChecklistView> publishGate(
            @PathVariable UUID templateId,
            @RequestParam(required = false) PublishGatePhase phase,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        PublishGatePhase effectivePhase = phase != null ? phase : PublishGatePhase.PUBLISH;
        return envelope(request, publishGateService.evaluate(templateId, session, effectivePhase));
    }

    @GetMapping("/{templateId}/release-versions")
    public SuccessEnvelope<List<TemplateReleaseVersionView>> listReleaseVersions(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.listReleaseVersions(templateId, session));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessEnvelope<TemplateDetailView> create(
            @Valid @RequestBody CreateTemplateRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.create(body, session));
    }

    @PatchMapping("/{templateId}")
    public SuccessEnvelope<TemplateDetailView> updateMetadata(
            @PathVariable UUID templateId,
            @Valid @RequestBody UpdateTemplateRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, templateService.updateMetadata(templateId, body, session));
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody LifecycleGovernanceRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session
    ) {
        templateDeleteService.deleteTemplate(templateId, body, session);
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
