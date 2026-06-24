package com.bank.docgen.runtime.web;

import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.runtime.service.DocumentDownloadService;
import com.bank.docgen.runtime.service.DocumentDownloadService.DownloadArtifact;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{environment}/v1/documents/{documentId}")
public class RuntimeDocumentController {

    private final DocumentDownloadService documentDownloadService;

    public RuntimeDocumentController(DocumentDownloadService documentDownloadService) {
        this.documentDownloadService = documentDownloadService;
    }

    @GetMapping("/download")
    public void download(
            @PathVariable String environment,
            @PathVariable String documentId,
            @AuthenticationPrincipal RuntimeSessionClaims session,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        try (DownloadArtifact artifact = documentDownloadService.resolveDownload(
                documentId,
                environment,
                session,
                request
        )) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(artifact.contentType());
            response.setHeader("documentId", artifact.documentId());
            response.setHeader("auditId", artifact.auditId());
            response.setHeader("traceId", artifact.traceId());
            response.setHeader("download.expiresAt", artifact.downloadExpiresAt().toString());
            artifact.contentStream().transferTo(response.getOutputStream());
        }
    }
}
