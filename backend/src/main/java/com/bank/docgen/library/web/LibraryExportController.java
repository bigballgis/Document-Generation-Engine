package com.bank.docgen.library.web;

import com.bank.docgen.library.api.LibraryExportRequest;
import com.bank.docgen.library.service.LibraryExportService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/management/v1/library")
public class LibraryExportController {

    private final LibraryExportService libraryExportService;

    public LibraryExportController(LibraryExportService libraryExportService) {
        this.libraryExportService = libraryExportService;
    }

    @PostMapping(value = "/export", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE})
    public void exportLibrary(
            @RequestBody(required = false) LibraryExportRequest request,
            @AuthenticationPrincipal ManagementSessionClaims session,
            HttpServletResponse response
    ) throws IOException {
        LibraryExportService.LibraryExportZipArtifact artifact =
                libraryExportService.exportLibrary(request, session);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + artifact.filename() + "\""
        );
        response.getOutputStream().write(artifact.content());
    }
}
