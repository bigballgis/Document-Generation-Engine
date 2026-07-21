package com.bank.docgen.library.web;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.library.api.AssetLibraryAssetView;
import com.bank.docgen.library.domain.AssetLibraryAssetClass;
import com.bank.docgen.library.service.AssetLibraryListStatusFilter;
import com.bank.docgen.library.service.AssetLibraryService;
import com.bank.docgen.sharedkernel.api.Metadata;
import com.bank.docgen.sharedkernel.api.SuccessEnvelope;
import com.bank.docgen.sharedkernel.api.TraceIdProvider;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/management/v1/library/assets")
public class AssetLibraryController {

    private final AssetLibraryService assetLibraryService;
    private final TraceIdProvider traceIdProvider;

    public AssetLibraryController(AssetLibraryService assetLibraryService, TraceIdProvider traceIdProvider) {
        this.assetLibraryService = assetLibraryService;
        this.traceIdProvider = traceIdProvider;
    }

    @GetMapping
    public SuccessEnvelope<PageView<AssetLibraryAssetView>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) AssetLibraryAssetClass assetClass,
            @RequestParam(required = false) AssetLibraryListStatusFilter status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String groupCode,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, assetLibraryService.list(session, page, size, assetClass, status, q, groupCode));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessEnvelope<AssetLibraryAssetView> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("assetKey") String assetKey,
            @RequestParam("assetClass") AssetLibraryAssetClass assetClass,
            @RequestParam(value = "groupCode", required = false) String groupCode,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, assetLibraryService.upload(session, file, assetKey, assetClass, groupCode));
    }

    @PostMapping("/{assetKey}/disable")
    public SuccessEnvelope<AssetLibraryAssetView> disable(
            @PathVariable String assetKey,
            @RequestParam(value = "groupCode", required = false) String groupCode,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletRequest request
    ) {
        return envelope(request, assetLibraryService.disable(session, assetKey, groupCode));
    }

    private <T> SuccessEnvelope<T> envelope(HttpServletRequest request, T result) {
        String traceId = traceIdProvider.currentOrNew(request.getHeader("X-Trace-Id"));
        String auditId = traceIdProvider.newAuditId();
        return new SuccessEnvelope<>(Metadata.minimal(auditId, traceId), result);
    }
}
