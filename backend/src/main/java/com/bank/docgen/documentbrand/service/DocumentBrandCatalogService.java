package com.bank.docgen.documentbrand.service;

import com.bank.docgen.audit.service.ManagementAuditEventTypes;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.documentbrand.api.CreateDocumentBrandRequest;
import com.bank.docgen.documentbrand.api.DocumentBrandView;
import com.bank.docgen.documentbrand.api.UpdateDocumentBrandRequest;
import com.bank.docgen.documentbrand.domain.DocumentBrandStatus;
import com.bank.docgen.documentbrand.persistence.DocumentBrandEntity;
import com.bank.docgen.documentbrand.persistence.DocumentBrandRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentBrandCatalogService {

    private final DocumentBrandRepository documentBrandRepository;
    private final GroupAccessService groupAccessService;
    private final ManagementAuditRecorder auditRecorder;
    private final DocumentBrandSeedSupport seedSupport;

    public DocumentBrandCatalogService(
            DocumentBrandRepository documentBrandRepository,
            GroupAccessService groupAccessService,
            ManagementAuditRecorder auditRecorder,
            DocumentBrandSeedSupport seedSupport
    ) {
        this.documentBrandRepository = documentBrandRepository;
        this.groupAccessService = groupAccessService;
        this.auditRecorder = auditRecorder;
        this.seedSupport = seedSupport;
    }

    @Transactional(readOnly = true)
    public PageView<DocumentBrandView> list(
            ManagementSessionClaims session,
            String groupCode,
            String statusFilter
    ) {
        String group = requireReadableGroup(session, groupCode);
        seedSupport.ensurePlatformDefault(group);
        List<DocumentBrandEntity> rows = resolveList(group, statusFilter);
        return PageView.of(rows.stream().map(DocumentBrandCatalogService::toView).toList(), 0, Math.max(rows.size(), 1));
    }

    @Transactional(readOnly = true)
    public DocumentBrandView get(ManagementSessionClaims session, String groupCode, String documentBrandCode) {
        String group = requireReadableGroup(session, groupCode);
        String code = DocumentBrandCodes.requireValidCode(
                documentBrandCode,
                "api.error.documentBrand.codeInvalid"
        );
        DocumentBrandEntity entity = documentBrandRepository
                .findByGroupCodeAndDocumentBrandCodeAndDeletedAtIsNull(group, code)
                .orElseThrow(() -> new DocumentBrandCatalogException(
                        ApiErrorCodes.DOCUMENT_BRAND_UNKNOWN,
                        "api.error.documentBrand.documentBrandUnknown"
                ));
        return toView(entity);
    }

    @Transactional
    public DocumentBrandView create(ManagementSessionClaims session, CreateDocumentBrandRequest request) {
        requireAdmin(session);
        String group = requireWritableGroup(session, request.groupCode());
        String code = DocumentBrandCodes.requireValidCode(
                request.documentBrandCode(),
                "api.error.documentBrand.codeInvalid"
        );
        if (documentBrandRepository.existsByGroupCodeAndDocumentBrandCodeAndDeletedAtIsNull(group, code)) {
            throw new DocumentBrandCatalogException(
                    ApiErrorCodes.DOCUMENT_BRAND_CODE_INVALID,
                    "api.error.documentBrand.codeConflict"
            );
        }
        String logo = requireNonBlank(request.logoObjectRef(), "api.error.documentBrand.logoRequired");
        DocumentBrandEntity saved = documentBrandRepository.save(new DocumentBrandEntity(
                UUID.randomUUID(),
                group,
                code,
                request.displayName().trim(),
                request.status() == null ? DocumentBrandStatus.ACTIVE : request.status(),
                logo.trim(),
                DocumentBrandCodes.normalizeOptional(request.defaultSealObjectRef()),
                DocumentBrandCodes.normalizeOptional(request.letterheadLegalName())
        ));
        auditRecorder.recordGroupEvent(
                ManagementAuditEventTypes.DOCUMENT_BRAND_CREATED,
                group,
                session.username(),
                session.username(),
                "Created documentBrandCode=" + code
        );
        return toView(saved);
    }

    @Transactional
    public DocumentBrandView update(
            ManagementSessionClaims session,
            String documentBrandCode,
            UpdateDocumentBrandRequest request
    ) {
        requireAdmin(session);
        String group = requireWritableGroup(session, request.groupCode());
        String code = DocumentBrandCodes.requireValidCode(
                documentBrandCode,
                "api.error.documentBrand.codeInvalid"
        );
        DocumentBrandEntity entity = documentBrandRepository
                .findByGroupCodeAndDocumentBrandCodeAndDeletedAtIsNull(group, code)
                .orElseThrow(() -> new DocumentBrandCatalogException(
                        ApiErrorCodes.DOCUMENT_BRAND_UNKNOWN,
                        "api.error.documentBrand.documentBrandUnknown"
                ));
        String logo = request.logoObjectRef() == null
                ? entity.getLogoObjectRef()
                : requireNonBlank(request.logoObjectRef(), "api.error.documentBrand.logoRequired").trim();
        entity.update(
                request.displayName() == null ? null : request.displayName().trim(),
                request.status(),
                logo,
                request.defaultSealObjectRef() == null
                        ? entity.getDefaultSealObjectRef()
                        : DocumentBrandCodes.normalizeOptional(request.defaultSealObjectRef()),
                request.letterheadLegalName() == null
                        ? entity.getLetterheadLegalName()
                        : DocumentBrandCodes.normalizeOptional(request.letterheadLegalName())
        );
        DocumentBrandEntity saved = documentBrandRepository.save(entity);
        auditRecorder.recordGroupEvent(
                ManagementAuditEventTypes.DOCUMENT_BRAND_UPDATED,
                group,
                session.username(),
                session.username(),
                "Updated documentBrandCode=" + code + " status=" + saved.getStatus()
        );
        return toView(saved);
    }

    private List<DocumentBrandEntity> resolveList(String group, String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter.trim())) {
            return documentBrandRepository.findByGroupCodeAndDeletedAtIsNullOrderByDocumentBrandCodeAsc(group);
        }
        DocumentBrandStatus status = DocumentBrandStatus.valueOf(statusFilter.trim().toUpperCase(Locale.ROOT));
        return documentBrandRepository.findByGroupCodeAndStatusAndDeletedAtIsNullOrderByDocumentBrandCodeAsc(
                group, status
        );
    }

    private String requireReadableGroup(ManagementSessionClaims session, String groupCode) {
        if (groupCode == null || groupCode.isBlank()) {
            throw new DocumentBrandCatalogException(
                    ApiErrorCodes.REQUEST_BODY_INVALID,
                    "api.error.validation.requestBodyInvalid"
            );
        }
        String group = groupCode.trim();
        if (!groupAccessService.canAccessGroup(session, group)) {
            throw new DocumentBrandCatalogException(
                    ApiErrorCodes.ACCESS_DENIED,
                    "api.error.authorization.accessDenied"
            );
        }
        return group;
    }

    private String requireWritableGroup(ManagementSessionClaims session, String groupCode) {
        return requireReadableGroup(session, groupCode);
    }

    private static void requireAdmin(ManagementSessionClaims session) {
        if (!session.roles().contains("GLOBAL_ADMIN") && !session.roles().contains("GROUP_ADMIN")) {
            throw new DocumentBrandCatalogException(
                    ApiErrorCodes.ACCESS_DENIED,
                    "api.error.authorization.accessDenied"
            );
        }
    }

    private static String requireNonBlank(String value, String messageKey) {
        if (value == null || value.isBlank()) {
            throw new DocumentBrandCatalogException(ApiErrorCodes.REQUEST_BODY_INVALID, messageKey);
        }
        return value;
    }

    private static DocumentBrandView toView(DocumentBrandEntity entity) {
        return new DocumentBrandView(
                entity.getGroupCode(),
                entity.getDocumentBrandCode(),
                entity.getDisplayName(),
                entity.getStatus(),
                entity.getLogoObjectRef(),
                entity.getDefaultSealObjectRef(),
                entity.getLetterheadLegalName()
        );
    }
}
