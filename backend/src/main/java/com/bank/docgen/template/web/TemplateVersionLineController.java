package com.bank.docgen.template.web;



import com.bank.docgen.authorization.management.api.PageView;

import com.bank.docgen.sharedkernel.api.Metadata;

import com.bank.docgen.sharedkernel.api.SuccessEnvelope;

import com.bank.docgen.sharedkernel.api.TraceIdProvider;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;

import com.bank.docgen.template.api.TemplateDetailView;

import com.bank.docgen.template.api.TemplateDevVersionCreatedView;

import com.bank.docgen.template.api.TemplateVersionLineDetailView;

import com.bank.docgen.template.api.TemplateVersionLineSummaryView;

import com.bank.docgen.template.service.TemplateVersionLineService;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

import org.springframework.http.HttpStatus;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.ResponseStatus;

import org.springframework.web.bind.annotation.RestController;



@RestController

@RequestMapping("/api/management/v1/templates/{templateId}")

public class TemplateVersionLineController {



    private final TemplateVersionLineService templateVersionLineService;

    private final TraceIdProvider traceIdProvider;



    public TemplateVersionLineController(

            TemplateVersionLineService templateVersionLineService,

            TraceIdProvider traceIdProvider

    ) {

        this.templateVersionLineService = templateVersionLineService;

        this.traceIdProvider = traceIdProvider;

    }



    @GetMapping("/version-lines")

    public SuccessEnvelope<PageView<TemplateVersionLineSummaryView>> list(

            @PathVariable UUID templateId,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "20") int size,

            @AuthenticationPrincipal ManagementSessionClaims session,

            HttpServletRequest request

    ) {

        return envelope(request, templateVersionLineService.list(templateId, page, size, session));

    }



    @GetMapping("/version-lines/{versionLineId}")

    public SuccessEnvelope<TemplateVersionLineDetailView> getVersionLine(

            @PathVariable UUID templateId,

            @PathVariable UUID versionLineId,

            @AuthenticationPrincipal ManagementSessionClaims session,

            HttpServletRequest request

    ) {

        return envelope(request, templateVersionLineService.get(templateId, versionLineId, session));

    }



    @GetMapping("/dev/{devVersionId}")

    public SuccessEnvelope<TemplateDetailView> getDevDetail(

            @PathVariable UUID templateId,

            @PathVariable UUID devVersionId,

            @AuthenticationPrincipal ManagementSessionClaims session,

            HttpServletRequest request

    ) {

        return envelope(request, templateVersionLineService.getDevDetail(templateId, devVersionId, session));

    }



    @GetMapping("/releases/{releaseVersion}")

    public SuccessEnvelope<TemplateDetailView> getReleaseDetail(

            @PathVariable UUID templateId,

            @PathVariable String releaseVersion,

            @AuthenticationPrincipal ManagementSessionClaims session,

            HttpServletRequest request

    ) {

        return envelope(request, templateVersionLineService.getReleaseDetail(templateId, releaseVersion, session));

    }



    @PostMapping("/release-versions/{releaseVersion}/clone")

    @ResponseStatus(HttpStatus.CREATED)

    public SuccessEnvelope<TemplateDevVersionCreatedView> cloneReleaseVersion(

            @PathVariable UUID templateId,

            @PathVariable String releaseVersion,

            @AuthenticationPrincipal ManagementSessionClaims session,

            HttpServletRequest request

    ) {

        return envelope(request, templateVersionLineService.cloneReleaseVersion(templateId, releaseVersion, session));

    }



    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {

        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));

        String auditId = traceIdProvider.newAuditId();

        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);

    }

}


