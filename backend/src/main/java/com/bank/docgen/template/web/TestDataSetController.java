package com.bank.docgen.template.web;

import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.TestDataSetView;
import com.bank.docgen.template.api.UpsertTestDataSetRequest;
import com.bank.docgen.template.service.TestDataSetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/templates/{templateId}/test-data-sets")
public class TestDataSetController {

    private final TestDataSetService testDataSetService;
    private final TraceIdProvider traceIdProvider;

    public TestDataSetController(TestDataSetService testDataSetService, TraceIdProvider traceIdProvider) {
        this.testDataSetService = testDataSetService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<List<TestDataSetView>> list(
            @PathVariable UUID templateId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, testDataSetService.list(templateId, session));
    }

    @GetMapping("/{testDataSetId}")
    public SuccessEnvelope<TestDataSetView> get(
            @PathVariable UUID templateId,
            @PathVariable String testDataSetId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, testDataSetService.get(templateId, testDataSetId, session));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessEnvelope<TestDataSetView> create(
            @PathVariable UUID templateId,
            @Valid @RequestBody UpsertTestDataSetRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, testDataSetService.create(templateId, body, session));
    }

    @PutMapping("/{testDataSetId}")
    public SuccessEnvelope<TestDataSetView> update(
            @PathVariable UUID templateId,
            @PathVariable String testDataSetId,
            @Valid @RequestBody UpsertTestDataSetRequest body,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, testDataSetService.update(templateId, testDataSetId, body, session));
    }

    @PostMapping("/{testDataSetId}/derive")
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessEnvelope<TestDataSetView> derive(
            @PathVariable UUID templateId,
            @PathVariable String testDataSetId,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, testDataSetService.derive(templateId, testDataSetId, session));
    }

    @DeleteMapping("/{testDataSetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID templateId,
            @PathVariable String testDataSetId,
            @AuthenticationPrincipal ManagementSessionClaims session
    ) {
        testDataSetService.delete(templateId, testDataSetId, session);
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
