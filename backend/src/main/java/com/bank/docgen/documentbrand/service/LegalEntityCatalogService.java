package com.bank.docgen.documentbrand.service;

import com.bank.docgen.audit.service.ManagementAuditEventTypes;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.api.PageView;
import com.bank.docgen.authorization.management.persistence.BusinessGroupEntity;
import com.bank.docgen.authorization.management.persistence.BusinessGroupRepository;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.documentbrand.api.CreateLegalEntityRequest;
import com.bank.docgen.documentbrand.api.GroupDefaultLegalEntityView;
import com.bank.docgen.documentbrand.api.LegalEntityView;
import com.bank.docgen.documentbrand.api.PutGroupDefaultLegalEntityRequest;
import com.bank.docgen.documentbrand.api.UpdateLegalEntityRequest;
import com.bank.docgen.documentbrand.domain.DocumentBrandStatus;
import com.bank.docgen.documentbrand.persistence.DocumentBrandEntity;
import com.bank.docgen.documentbrand.persistence.DocumentBrandRepository;
import com.bank.docgen.documentbrand.persistence.LegalEntityEntity;
import com.bank.docgen.documentbrand.persistence.LegalEntityRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegalEntityCatalogService {

    private final LegalEntityRepository legalEntityRepository;
    private final DocumentBrandRepository documentBrandRepository;
    private final BusinessGroupRepository businessGroupRepository;
    private final GroupAccessService groupAccessService;
    private final ManagementAuditRecorder auditRecorder;
    private final DocumentBrandSeedSupport seedSupport;

    public LegalEntityCatalogService(
            LegalEntityRepository legalEntityRepository,
            DocumentBrandRepository documentBrandRepository,
            BusinessGroupRepository businessGroupRepository,
            GroupAccessService groupAccessService,
            ManagementAuditRecorder auditRecorder,
            DocumentBrandSeedSupport seedSupport
    ) {
        this.legalEntityRepository = legalEntityRepository;
        this.documentBrandRepository = documentBrandRepository;
        this.businessGroupRepository = businessGroupRepository;
        this.groupAccessService = groupAccessService;
        this.auditRecorder = auditRecorder;
        this.seedSupport = seedSupport;
    }

    @Transactional(readOnly = true)
    public PageView<LegalEntityView> list(
            ManagementSessionClaims session,
            String groupCode,
            String statusFilter
    ) {
        String group = requireReadableGroup(session, groupCode);
        seedSupport.ensurePlatformDefault(group);
        List<LegalEntityEntity> rows = resolveList(group, statusFilter);
        return PageView.of(rows.stream().map(LegalEntityCatalogService::toView).toList(), 0, Math.max(rows.size(), 1));
    }

    @Transactional(readOnly = true)
    public LegalEntityView get(ManagementSessionClaims session, String groupCode, String legalEntityCode) {
        String group = requireReadableGroup(session, groupCode);
        String code = DocumentBrandCodes.requireValidCode(
                legalEntityCode,
                "api.error.documentBrand.codeInvalid"
        );
        LegalEntityEntity entity = legalEntityRepository
                .findByGroupCodeAndLegalEntityCodeAndDeletedAtIsNull(group, code)
                .orElseThrow(() -> new DocumentBrandCatalogException(
                        ApiErrorCodes.LEGAL_ENTITY_UNKNOWN,
                        "api.error.documentBrand.legalEntityUnknown"
                ));
        return toView(entity);
    }

    @Transactional
    public LegalEntityView create(ManagementSessionClaims session, CreateLegalEntityRequest request) {
        requireAdmin(session);
        String group = requireWritableGroup(session, request.groupCode());
        String code = DocumentBrandCodes.requireValidCode(
                request.legalEntityCode(),
                "api.error.documentBrand.codeInvalid"
        );
        if (request.documentBrandCode() == null || request.documentBrandCode().isBlank()) {
            throw new DocumentBrandCatalogException(
                    ApiErrorCodes.REQUEST_BODY_INVALID,
                    "api.error.documentBrand.documentBrandRequired"
            );
        }
        String brandCode = DocumentBrandCodes.requireValidCode(
                request.documentBrandCode(),
                "api.error.documentBrand.codeInvalid"
        );
        requireExistingBrand(group, brandCode);
        if (legalEntityRepository.existsByGroupCodeAndLegalEntityCodeAndDeletedAtIsNull(group, code)) {
            throw new DocumentBrandCatalogException(
                    ApiErrorCodes.DOCUMENT_BRAND_CODE_INVALID,
                    "api.error.documentBrand.codeConflict"
            );
        }
        LegalEntityEntity saved = legalEntityRepository.save(new LegalEntityEntity(
                UUID.randomUUID(),
                group,
                code,
                request.displayName().trim(),
                request.status() == null ? DocumentBrandStatus.ACTIVE : request.status(),
                brandCode
        ));
        auditRecorder.recordGroupEvent(
                ManagementAuditEventTypes.LEGAL_ENTITY_CREATED,
                group,
                session.username(),
                session.username(),
                "Created legalEntityCode=" + code + " documentBrandCode=" + brandCode
        );
        return toView(saved);
    }

    @Transactional
    public LegalEntityView update(
            ManagementSessionClaims session,
            String legalEntityCode,
            UpdateLegalEntityRequest request
    ) {
        requireAdmin(session);
        String group = requireWritableGroup(session, request.groupCode());
        String code = DocumentBrandCodes.requireValidCode(
                legalEntityCode,
                "api.error.documentBrand.codeInvalid"
        );
        LegalEntityEntity entity = legalEntityRepository
                .findByGroupCodeAndLegalEntityCodeAndDeletedAtIsNull(group, code)
                .orElseThrow(() -> new DocumentBrandCatalogException(
                        ApiErrorCodes.LEGAL_ENTITY_UNKNOWN,
                        "api.error.documentBrand.legalEntityUnknown"
                ));
        String previousBrand = entity.getDocumentBrandCode();
        String brandCode = request.documentBrandCode() == null
                ? previousBrand
                : DocumentBrandCodes.requireValidCode(
                        request.documentBrandCode(),
                        "api.error.documentBrand.codeInvalid"
                );
        if (request.documentBrandCode() != null) {
            requireExistingBrand(group, brandCode);
        }
        entity.update(
                request.displayName() == null ? null : request.displayName().trim(),
                request.status(),
                brandCode
        );
        LegalEntityEntity saved = legalEntityRepository.save(entity);
        String summary = "Updated legalEntityCode=" + code;
        if (!previousBrand.equals(saved.getDocumentBrandCode())) {
            summary = summary + " rebind " + previousBrand + "->" + saved.getDocumentBrandCode();
        }
        auditRecorder.recordGroupEvent(
                ManagementAuditEventTypes.LEGAL_ENTITY_UPDATED,
                group,
                session.username(),
                session.username(),
                summary
        );
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public GroupDefaultLegalEntityView getDefault(ManagementSessionClaims session, String groupCode) {
        String group = requireReadableGroup(session, groupCode);
        BusinessGroupEntity entity = businessGroupRepository.findByGroupCodeAndDeletedAtIsNull(group)
                .orElseThrow(() -> new DocumentBrandCatalogException(
                        ApiErrorCodes.GROUP_NOT_FOUND,
                        "api.error.notFound.groupNotFound"
                ));
        return new GroupDefaultLegalEntityView(group, entity.getDefaultLegalEntityCode());
    }

    @Transactional
    public GroupDefaultLegalEntityView putDefault(
            ManagementSessionClaims session,
            String groupCode,
            PutGroupDefaultLegalEntityRequest request
    ) {
        requireAdmin(session);
        String group = requireWritableGroup(session, groupCode);
        BusinessGroupEntity entity = businessGroupRepository.findByGroupCodeAndDeletedAtIsNull(group)
                .orElseThrow(() -> new DocumentBrandCatalogException(
                        ApiErrorCodes.GROUP_NOT_FOUND,
                        "api.error.notFound.groupNotFound"
                ));
        String defaultCode = DocumentBrandCodes.normalizeOptional(
                request == null ? null : request.defaultLegalEntityCode()
        );
        if (defaultCode != null) {
            defaultCode = DocumentBrandCodes.requireValidCode(
                    defaultCode,
                    "api.error.documentBrand.codeInvalid"
            );
            legalEntityRepository.findByGroupCodeAndLegalEntityCodeAndDeletedAtIsNull(group, defaultCode)
                    .orElseThrow(() -> new DocumentBrandCatalogException(
                            ApiErrorCodes.LEGAL_ENTITY_UNKNOWN,
                            "api.error.documentBrand.legalEntityUnknown"
                    ));
        }
        entity.setDefaultLegalEntityCode(defaultCode);
        businessGroupRepository.save(entity);
        auditRecorder.recordGroupEvent(
                ManagementAuditEventTypes.GROUP_DEFAULT_LEGAL_ENTITY_UPDATED,
                group,
                session.username(),
                session.username(),
                "defaultLegalEntityCode=" + (defaultCode == null ? "null" : defaultCode)
        );
        return new GroupDefaultLegalEntityView(group, defaultCode);
    }

    private void requireExistingBrand(String group, String brandCode) {
        DocumentBrandEntity brand = documentBrandRepository
                .findByGroupCodeAndDocumentBrandCodeAndDeletedAtIsNull(group, brandCode)
                .orElseThrow(() -> new DocumentBrandCatalogException(
                        ApiErrorCodes.DOCUMENT_BRAND_UNKNOWN,
                        "api.error.documentBrand.documentBrandUnknown"
                ));
        if (brand.getStatus() != DocumentBrandStatus.ACTIVE) {
            throw new DocumentBrandCatalogException(
                    ApiErrorCodes.DOCUMENT_BRAND_INACTIVE,
                    "api.error.documentBrand.documentBrandInactive"
            );
        }
    }

    private List<LegalEntityEntity> resolveList(String group, String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank() || "ALL".equalsIgnoreCase(statusFilter.trim())) {
            return legalEntityRepository.findByGroupCodeAndDeletedAtIsNullOrderByLegalEntityCodeAsc(group);
        }
        DocumentBrandStatus status = DocumentBrandStatus.valueOf(statusFilter.trim().toUpperCase(Locale.ROOT));
        return legalEntityRepository.findByGroupCodeAndStatusAndDeletedAtIsNullOrderByLegalEntityCodeAsc(
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

    private static LegalEntityView toView(LegalEntityEntity entity) {
        return new LegalEntityView(
                entity.getGroupCode(),
                entity.getLegalEntityCode(),
                entity.getDisplayName(),
                entity.getStatus(),
                entity.getDocumentBrandCode()
        );
    }
}
