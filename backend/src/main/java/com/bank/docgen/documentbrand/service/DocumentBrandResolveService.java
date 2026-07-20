package com.bank.docgen.documentbrand.service;

import com.bank.docgen.authorization.management.persistence.BusinessGroupEntity;
import com.bank.docgen.authorization.management.persistence.BusinessGroupRepository;
import com.bank.docgen.documentbrand.domain.DocumentBrandStatus;
import com.bank.docgen.documentbrand.domain.ResolvedDocumentBrand;
import com.bank.docgen.documentbrand.persistence.DocumentBrandEntity;
import com.bank.docgen.documentbrand.persistence.DocumentBrandRepository;
import com.bank.docgen.documentbrand.persistence.LegalEntityEntity;
import com.bank.docgen.documentbrand.persistence.LegalEntityRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADR-0065 / IBL-E4 — deterministic LegalEntity → DocumentBrand resolve (runtime + preview/test).
 */
@Service
public class DocumentBrandResolveService {

    private final DocumentBrandRepository documentBrandRepository;
    private final LegalEntityRepository legalEntityRepository;
    private final BusinessGroupRepository businessGroupRepository;
    private final DocumentBrandSeedSupport seedSupport;

    public DocumentBrandResolveService(
            DocumentBrandRepository documentBrandRepository,
            LegalEntityRepository legalEntityRepository,
            BusinessGroupRepository businessGroupRepository,
            DocumentBrandSeedSupport seedSupport
    ) {
        this.documentBrandRepository = documentBrandRepository;
        this.legalEntityRepository = legalEntityRepository;
        this.businessGroupRepository = businessGroupRepository;
        this.seedSupport = seedSupport;
    }

    @Transactional(readOnly = true)
    public ResolvedDocumentBrand resolve(
            String groupCode,
            String legalEntityCodeRaw,
            Collection<String> allowedDocumentBrandCodes
    ) {
        String group = requireGroupCode(groupCode);
        seedSupport.ensurePlatformDefault(group);
        String requestedEntity = DocumentBrandCodes.normalizeOptional(legalEntityCodeRaw);
        ResolvedDocumentBrand resolved = requestedEntity == null
                ? resolveOmittedLegalEntity(group)
                : resolveExplicitLegalEntity(group, requestedEntity);
        assertAllowList(resolved.documentBrandCode(), allowedDocumentBrandCodes);
        return resolved;
    }

    private ResolvedDocumentBrand resolveExplicitLegalEntity(String groupCode, String legalEntityCode) {
        LegalEntityEntity entity = legalEntityRepository
                .findByGroupCodeAndLegalEntityCodeAndDeletedAtIsNull(groupCode, legalEntityCode)
                .orElseThrow(() -> new DocumentBrandResolveException(
                        ApiErrorCodes.LEGAL_ENTITY_UNKNOWN,
                        "api.error.documentBrand.legalEntityUnknown"
                ));
        if (entity.getStatus() != DocumentBrandStatus.ACTIVE) {
            throw new DocumentBrandResolveException(
                    ApiErrorCodes.LEGAL_ENTITY_INACTIVE,
                    "api.error.documentBrand.legalEntityInactive"
            );
        }
        DocumentBrandEntity brand = requireActiveBrand(groupCode, entity.getDocumentBrandCode());
        return toResolved(entity.getLegalEntityCode(), brand);
    }

    private ResolvedDocumentBrand resolveOmittedLegalEntity(String groupCode) {
        BusinessGroupEntity group = businessGroupRepository.findByGroupCodeAndDeletedAtIsNull(groupCode)
                .orElse(null);
        String defaultEntityCode = group == null
                ? null
                : DocumentBrandCodes.normalizeOptional(group.getDefaultLegalEntityCode());
        if (defaultEntityCode != null) {
            LegalEntityEntity entity = legalEntityRepository
                    .findByGroupCodeAndLegalEntityCodeAndDeletedAtIsNull(groupCode, defaultEntityCode)
                    .orElse(null);
            if (entity != null && entity.getStatus() == DocumentBrandStatus.ACTIVE) {
                DocumentBrandEntity brand = requireActiveBrand(groupCode, entity.getDocumentBrandCode());
                return toResolved(entity.getLegalEntityCode(), brand);
            }
        }
        DocumentBrandEntity platformDefault = requireActiveBrand(groupCode, DocumentBrandCodes.PLATFORM_DEFAULT);
        return toResolved(null, platformDefault);
    }

    private DocumentBrandEntity requireActiveBrand(String groupCode, String documentBrandCode) {
        DocumentBrandEntity brand = documentBrandRepository
                .findByGroupCodeAndDocumentBrandCodeAndDeletedAtIsNull(groupCode, documentBrandCode)
                .orElseThrow(() -> new DocumentBrandResolveException(
                        ApiErrorCodes.DOCUMENT_BRAND_INACTIVE,
                        "api.error.documentBrand.documentBrandInactive"
                ));
        if (brand.getStatus() != DocumentBrandStatus.ACTIVE) {
            throw new DocumentBrandResolveException(
                    ApiErrorCodes.DOCUMENT_BRAND_INACTIVE,
                    "api.error.documentBrand.documentBrandInactive"
            );
        }
        return brand;
    }

    private static void assertAllowList(String documentBrandCode, Collection<String> allowedDocumentBrandCodes) {
        if (allowedDocumentBrandCodes == null || allowedDocumentBrandCodes.isEmpty()) {
            return;
        }
        List<String> allowed = allowedDocumentBrandCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .toList();
        if (allowed.isEmpty()) {
            return;
        }
        if (!allowed.contains(documentBrandCode)) {
            throw new DocumentBrandResolveException(
                    ApiErrorCodes.DOCUMENT_BRAND_NOT_ALLOWED,
                    "api.error.documentBrand.documentBrandNotAllowed"
            );
        }
    }

    private static ResolvedDocumentBrand toResolved(String legalEntityCode, DocumentBrandEntity brand) {
        return new ResolvedDocumentBrand(
                legalEntityCode,
                brand.getDocumentBrandCode(),
                brand.getLogoObjectRef(),
                brand.getDefaultSealObjectRef(),
                brand.getLetterheadLegalName()
        );
    }

    private static String requireGroupCode(String groupCode) {
        if (groupCode == null || groupCode.isBlank()) {
            throw new DocumentBrandResolveException(
                    ApiErrorCodes.REQUEST_BODY_INVALID,
                    "api.error.validation.requestBodyInvalid"
            );
        }
        return groupCode.trim();
    }
}
