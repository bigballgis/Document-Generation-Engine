package com.bank.docgen.documentbrand.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bank.docgen.authorization.management.persistence.BusinessGroupEntity;
import com.bank.docgen.authorization.management.persistence.BusinessGroupRepository;
import com.bank.docgen.documentbrand.domain.DocumentBrandStatus;
import com.bank.docgen.documentbrand.domain.ResolvedDocumentBrand;
import com.bank.docgen.documentbrand.persistence.DocumentBrandEntity;
import com.bank.docgen.documentbrand.persistence.DocumentBrandRepository;
import com.bank.docgen.documentbrand.persistence.LegalEntityEntity;
import com.bank.docgen.documentbrand.persistence.LegalEntityRepository;
import com.bank.docgen.sharedkernel.api.ApiErrorCodes;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BDD-IBL-E4-005…007 / 010 / 011 — resolve + allow-list fail-closed rules.
 */
@ExtendWith(MockitoExtension.class)
class DocumentBrandResolveServiceTest {

    @Mock
    private DocumentBrandRepository documentBrandRepository;
    @Mock
    private LegalEntityRepository legalEntityRepository;
    @Mock
    private BusinessGroupRepository businessGroupRepository;
    @Mock
    private DocumentBrandSeedSupport seedSupport;

    private DocumentBrandResolveService service;

    @BeforeEach
    void setUp() {
        service = new DocumentBrandResolveService(
                documentBrandRepository,
                legalEntityRepository,
                businessGroupRepository,
                seedSupport
        );
    }

    @Test
    void unknownLegalEntity_failClosed_bddE4005() {
        when(legalEntityRepository.findByGroupCodeAndLegalEntityCodeAndDeletedAtIsNull("RETAIL", "NO-SUCH-ENTITY"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolve("RETAIL", "NO-SUCH-ENTITY", List.of()))
                .isInstanceOf(DocumentBrandResolveException.class)
                .satisfies(ex -> assertThat(((DocumentBrandResolveException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.LEGAL_ENTITY_UNKNOWN));
    }

    @Test
    void inactiveLegalEntity_failClosed_bddE4006() {
        LegalEntityEntity entity = legalEntity("LE-HK-001", "HK-RETAIL-LETTER", DocumentBrandStatus.INACTIVE);
        when(legalEntityRepository.findByGroupCodeAndLegalEntityCodeAndDeletedAtIsNull("RETAIL", "LE-HK-001"))
                .thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.resolve("RETAIL", "LE-HK-001", List.of()))
                .isInstanceOf(DocumentBrandResolveException.class)
                .satisfies(ex -> assertThat(((DocumentBrandResolveException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.LEGAL_ENTITY_INACTIVE));
    }

    @Test
    void inactiveBoundBrand_failClosed_bddE4006() {
        LegalEntityEntity entity = legalEntity("LE-HK-001", "HK-RETAIL-LETTER", DocumentBrandStatus.ACTIVE);
        when(legalEntityRepository.findByGroupCodeAndLegalEntityCodeAndDeletedAtIsNull("RETAIL", "LE-HK-001"))
                .thenReturn(Optional.of(entity));
        when(documentBrandRepository.findByGroupCodeAndDocumentBrandCodeAndDeletedAtIsNull(
                "RETAIL", "HK-RETAIL-LETTER"
        )).thenReturn(Optional.of(brand("HK-RETAIL-LETTER", DocumentBrandStatus.INACTIVE)));

        assertThatThrownBy(() -> service.resolve("RETAIL", "LE-HK-001", List.of()))
                .isInstanceOf(DocumentBrandResolveException.class)
                .satisfies(ex -> assertThat(((DocumentBrandResolveException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.DOCUMENT_BRAND_INACTIVE));
    }

    @Test
    void omitLegalEntity_usesPlatformDefault_bddE4007() {
        when(businessGroupRepository.findByGroupCodeAndDeletedAtIsNull("RETAIL"))
                .thenReturn(Optional.of(group(null)));
        when(documentBrandRepository.findByGroupCodeAndDocumentBrandCodeAndDeletedAtIsNull(
                "RETAIL", DocumentBrandCodes.PLATFORM_DEFAULT
        )).thenReturn(Optional.of(brand(DocumentBrandCodes.PLATFORM_DEFAULT, DocumentBrandStatus.ACTIVE)));

        ResolvedDocumentBrand resolved = service.resolve("RETAIL", null, List.of());

        assertThat(resolved.legalEntityCode()).isNull();
        assertThat(resolved.documentBrandCode()).isEqualTo(DocumentBrandCodes.PLATFORM_DEFAULT);
        assertThat(resolved.logoObjectRef()).isEqualTo("platform/document-brands/PLATFORM_DEFAULT/logo");
    }

    @Test
    void allowListRejectsDisallowedBrand_bddE4010() {
        LegalEntityEntity entity = legalEntity("LE-UK-001", "UK-CORP-LETTER", DocumentBrandStatus.ACTIVE);
        when(legalEntityRepository.findByGroupCodeAndLegalEntityCodeAndDeletedAtIsNull("RETAIL", "LE-UK-001"))
                .thenReturn(Optional.of(entity));
        when(documentBrandRepository.findByGroupCodeAndDocumentBrandCodeAndDeletedAtIsNull(
                "RETAIL", "UK-CORP-LETTER"
        )).thenReturn(Optional.of(brand("UK-CORP-LETTER", DocumentBrandStatus.ACTIVE)));

        assertThatThrownBy(() -> service.resolve("RETAIL", "LE-UK-001", List.of("HK-RETAIL-LETTER")))
                .isInstanceOf(DocumentBrandResolveException.class)
                .satisfies(ex -> assertThat(((DocumentBrandResolveException) ex).errorCode())
                        .isEqualTo(ApiErrorCodes.DOCUMENT_BRAND_NOT_ALLOWED));
    }

    @Test
    void emptyAllowListPermitsAnyActiveBrand_bddE4011() {
        LegalEntityEntity entity = legalEntity("LE-HK-001", "HK-RETAIL-LETTER", DocumentBrandStatus.ACTIVE);
        when(legalEntityRepository.findByGroupCodeAndLegalEntityCodeAndDeletedAtIsNull("RETAIL", "LE-HK-001"))
                .thenReturn(Optional.of(entity));
        when(documentBrandRepository.findByGroupCodeAndDocumentBrandCodeAndDeletedAtIsNull(
                "RETAIL", "HK-RETAIL-LETTER"
        )).thenReturn(Optional.of(brand("HK-RETAIL-LETTER", DocumentBrandStatus.ACTIVE)));

        ResolvedDocumentBrand resolved = service.resolve("RETAIL", "LE-HK-001", List.of());

        assertThat(resolved.documentBrandCode()).isEqualTo("HK-RETAIL-LETTER");
        assertThat(resolved.legalEntityCode()).isEqualTo("LE-HK-001");
    }

    private static LegalEntityEntity legalEntity(String code, String brand, DocumentBrandStatus status) {
        return new LegalEntityEntity(
                UUID.randomUUID(),
                "RETAIL",
                code,
                code,
                status,
                brand
        );
    }

    private static DocumentBrandEntity brand(String code, DocumentBrandStatus status) {
        return new DocumentBrandEntity(
                UUID.randomUUID(),
                "RETAIL",
                code,
                code,
                status,
                "platform/document-brands/" + code + "/logo",
                null,
                null
        );
    }

    private static BusinessGroupEntity group(String defaultLegalEntityCode) {
        BusinessGroupEntity entity = new BusinessGroupEntity(
                UUID.randomUUID(),
                "RETAIL",
                "Retail",
                com.bank.docgen.authorization.management.domain.GroupDimension.BUSINESS_LINE
        );
        entity.setDefaultLegalEntityCode(defaultLegalEntityCode);
        return entity;
    }
}
