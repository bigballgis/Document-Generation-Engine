package com.bank.docgen.template.web;

import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ImportTemplateRequest;
import com.bank.docgen.template.api.TemplateImportDryRunResult;
import com.bank.docgen.template.api.TemplateImportResult;
import com.bank.docgen.template.domain.TemplateImportConflictPolicy;
import com.bank.docgen.template.service.TemplateImportService;
import com.bank.docgen.template.service.TemplateValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/management/v1/templates")
public class TemplateImportController {

    private final TemplateImportService templateImportService;
    private final TraceIdProvider traceIdProvider;

    public TemplateImportController(
            TemplateImportService templateImportService,
            TraceIdProvider traceIdProvider
    ) {
        this.templateImportService = templateImportService;
        this.traceIdProvider = traceIdProvider;
    }

    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SuccessEnvelope<?>> importBundleJson(
            @Valid @RequestBody ImportTemplateRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        if (body.isDryRun()) {
            TemplateImportDryRunResult dryRun = templateImportService.dryRun(body, session);
            return ResponseEntity.ok(envelope(request, dryRun));
        }
        TemplateImportResult result = templateImportService.importBundle(body, session);
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(request, result));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuccessEnvelope<?>> importBundleMultipart(
            @RequestPart("masterId") String masterId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "importConflictPolicy", required = false) String importConflictPolicy,
            @RequestPart(value = "dryRun", required = false) String dryRun,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        byte[] zipBytes;
        try {
            zipBytes = file.getBytes();
        } catch (Exception ex) {
            throw new TemplateValidationException("api.error.template.importBundleInvalid");
        }
        TemplateImportService.ParsedZipImport parsed = templateImportService.parseZipBytes(zipBytes);
        TemplateImportConflictPolicy policy = null;
        if (importConflictPolicy != null && !importConflictPolicy.isBlank()) {
            try {
                policy = TemplateImportConflictPolicy.valueOf(importConflictPolicy.trim());
            } catch (IllegalArgumentException ex) {
                throw new TemplateValidationException("api.error.template.importBundleInvalid");
            }
        }
        boolean dryRunFlag = dryRun != null && ("true".equalsIgnoreCase(dryRun.trim()) || "1".equals(dryRun.trim()));
        ImportTemplateRequest body = new ImportTemplateRequest(
                masterId,
                parsed.bundle(),
                policy,
                dryRunFlag
        );
        if (dryRunFlag) {
            TemplateImportDryRunResult result = templateImportService.dryRun(
                    body,
                    session,
                    parsed.masterDocxBytes(),
                    true
            );
            return ResponseEntity.ok(envelope(request, result));
        }
        TemplateImportResult result = templateImportService.importBundle(
                body,
                session,
                parsed.masterDocxBytes(),
                true
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(envelope(request, result));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
