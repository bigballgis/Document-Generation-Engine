package com.bank.docgen.documentbrand.service;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.documentbrand.api.CreateDocumentBrandRequest;
import com.bank.docgen.documentbrand.api.DocumentBrandView;
import com.bank.docgen.documentbrand.api.UpdateDocumentBrandRequest;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * ADR-0071 / SYS-NORM Wave 6 — DocumentBrand management product surface retired
 * (BDD-SYS-NORM-D1-009).
 */
@Service
public class DocumentBrandCatalogService {

    private final GroupAccessService groupAccessService;

    public DocumentBrandCatalogService(GroupAccessService groupAccessService) {
        this.groupAccessService = Objects.requireNonNull(groupAccessService);
    }

    public PageView<DocumentBrandView> list(
            ManagementSessionClaims session,
            String groupCode,
            String statusFilter
    ) {
        throw retired();
    }

    public DocumentBrandView get(
            ManagementSessionClaims session,
            String groupCode,
            String documentBrandCode
    ) {
        throw retired();
    }

    public DocumentBrandView create(ManagementSessionClaims session, CreateDocumentBrandRequest request) {
        throw retired();
    }

    public DocumentBrandView update(
            ManagementSessionClaims session,
            String documentBrandCode,
            UpdateDocumentBrandRequest request
    ) {
        throw retired();
    }

    private DocumentBrandCatalogException retired() {
        // Keep GroupAccessService wiring for management authorization contract anchors.
        Objects.requireNonNull(groupAccessService);
        return new DocumentBrandCatalogException(
                ApiErrorCodes.DOCUMENT_BRAND_SURFACE_RETIRED,
                "api.error.documentBrand.surfaceRetired"
        );
    }
}
