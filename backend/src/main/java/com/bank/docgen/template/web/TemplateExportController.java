package com.bank.docgen.template.web;

import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TemplateExportResult;
import com.bank.docgen.template.domain.TemplateDependencyClosure;
import com.bank.docgen.template.service.TemplateExportService;
import com.bank.docgen.template.service.TemplateValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/templates")
public class TemplateExportController {

    private final TemplateExportService templateExportService;
    private final TraceIdProvider traceIdProvider;

    public TemplateExportController(
            TemplateExportService templateExportService,
            TraceIdProvider traceIdProvider
    ) {
        this.templateExportService = templateExportService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping(value = "/{templateId}/export", produces = MediaType.APPLICATION_JSON_VALUE)
    public SuccessEnvelope<TemplateExportResult> exportJson(
            @PathVariable UUID templateId,
            @RequestParam(value = "bundleVersion", required = false, defaultValue = "1") int bundleVersion,
            @RequestParam(value = "dependencyClosure", required = false) String dependencyClosure,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        TemplateDependencyClosure closure = parseDependencyClosure(dependencyClosure);
        // OpenAPI: PROMOTION requires format=zip — reject JSON carrier early with stable 4xx envelope.
        if (closure == TemplateDependencyClosure.PROMOTION) {
            throw new TemplateValidationException("api.error.template.exportFormatUnsupported");
        }
        return envelope(request, templateExportService.exportJson(
                templateId,
                session,
                bundleVersion,
                closure
        ));
    }

    @GetMapping(value = "/{templateId}/export", params = "format=zip")
    public void exportZip(
            @PathVariable UUID templateId,
            @RequestParam("format") String format,
            @RequestParam(value = "bundleVersion", required = false, defaultValue = "1") int bundleVersion,
            @RequestParam(value = "dependencyClosure", required = false) String dependencyClosure,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletResponse response
    ) throws IOException {
        if (!"zip".equalsIgnoreCase(format)) {
            throw new TemplateValidationException("api.error.template.exportFormatUnsupported");
        }
        TemplateExportService.TemplateExportZipArtifact artifact =
                templateExportService.exportZip(
                        templateId,
                        session,
                        bundleVersion,
                        parseDependencyClosure(dependencyClosure)
                );
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + artifact.filename() + "\""
        );
        response.getOutputStream().write(artifact.content());
    }

    private static TemplateDependencyClosure parseDependencyClosure(String raw) {
        try {
            return TemplateDependencyClosure.parseOptional(raw);
        } catch (IllegalArgumentException ex) {
            throw new TemplateValidationException("api.error.template.exportFormatUnsupported");
        }
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
