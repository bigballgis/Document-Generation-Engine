package com.bank.docgen.documentbrand.service;

import com.bank.docgen.documentbrand.domain.DocumentBrandStatus;
import com.bank.docgen.documentbrand.persistence.DocumentBrandEntity;
import com.bank.docgen.documentbrand.persistence.DocumentBrandRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ensures each group has seed {@code PLATFORM_DEFAULT} document brand (ADR-0065 E4-C5).
 */
@Component
public class DocumentBrandSeedSupport {

    private final DocumentBrandRepository documentBrandRepository;

    public DocumentBrandSeedSupport(DocumentBrandRepository documentBrandRepository) {
        this.documentBrandRepository = documentBrandRepository;
    }

    /**
     * Self-healing seed for groups that predate Flyway V72 or were created outside the migration path.
     * Uses {@code REQUIRES_NEW} so callers in read-only transactions (preview/generate) can still seed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensurePlatformDefault(String groupCode) {
        if (groupCode == null || groupCode.isBlank()) {
            return;
        }
        String group = groupCode.trim();
        if (documentBrandRepository.existsByGroupCodeAndDocumentBrandCodeAndDeletedAtIsNull(
                group, DocumentBrandCodes.PLATFORM_DEFAULT
        )) {
            return;
        }
        documentBrandRepository.save(new DocumentBrandEntity(
                UUID.randomUUID(),
                group,
                DocumentBrandCodes.PLATFORM_DEFAULT,
                DocumentBrandCodes.PLATFORM_DEFAULT_DISPLAY_NAME,
                DocumentBrandStatus.ACTIVE,
                DocumentBrandCodes.PLATFORM_DEFAULT_LOGO_REF,
                null,
                null
        ));
    }
}
