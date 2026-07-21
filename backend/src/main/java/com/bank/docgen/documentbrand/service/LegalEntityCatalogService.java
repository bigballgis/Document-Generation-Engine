package com.bank.docgen.documentbrand.service;

import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.documentbrand.api.CreateLegalEntityRequest;
import com.bank.docgen.documentbrand.api.GroupDefaultLegalEntityView;
import com.bank.docgen.documentbrand.api.LegalEntityView;
import com.bank.docgen.documentbrand.api.PutGroupDefaultLegalEntityRequest;
import com.bank.docgen.documentbrand.api.UpdateLegalEntityRequest;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * ADR-0071 / SYS-NORM Wave 6 — LegalEntity / defaultLegalEntity management product
 * surface retired (BDD-SYS-NORM-D1-010).
 */
@Service
public class LegalEntityCatalogService {

    private final GroupAccessService groupAccessService;

    public LegalEntityCatalogService(GroupAccessService groupAccessService) {
        this.groupAccessService = Objects.requireNonNull(groupAccessService);
    }

    public PageView<LegalEntityView> list(
            ManagementSessionClaims session,
            String groupCode,
            String statusFilter
    ) {
        throw retired();
    }

    public LegalEntityView get(
            ManagementSessionClaims session,
            String groupCode,
            String legalEntityCode
    ) {
        throw retired();
    }

    public LegalEntityView create(ManagementSessionClaims session, CreateLegalEntityRequest request) {
        throw retired();
    }

    public LegalEntityView update(
            ManagementSessionClaims session,
            String legalEntityCode,
            UpdateLegalEntityRequest request
    ) {
        throw retired();
    }

    public GroupDefaultLegalEntityView getDefault(ManagementSessionClaims session, String groupCode) {
        throw retired();
    }

    public GroupDefaultLegalEntityView putDefault(
            ManagementSessionClaims session,
            String groupCode,
            PutGroupDefaultLegalEntityRequest request
    ) {
        throw retired();
    }

    private DocumentBrandCatalogException retired() {
        // Keep GroupAccessService wiring for management authorization contract anchors.
        Objects.requireNonNull(groupAccessService);
        return new DocumentBrandCatalogException(
                ApiErrorCodes.LEGAL_ENTITY_SURFACE_RETIRED,
                "api.error.legalEntity.surfaceRetired"
        );
    }
}
