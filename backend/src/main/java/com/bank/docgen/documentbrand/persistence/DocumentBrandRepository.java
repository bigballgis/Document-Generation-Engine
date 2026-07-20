package com.bank.docgen.documentbrand.persistence;

import com.bank.docgen.documentbrand.domain.DocumentBrandStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentBrandRepository extends JpaRepository<DocumentBrandEntity, UUID> {

    Optional<DocumentBrandEntity> findByGroupCodeAndDocumentBrandCodeAndDeletedAtIsNull(
            String groupCode,
            String documentBrandCode
    );

    List<DocumentBrandEntity> findByGroupCodeAndDeletedAtIsNullOrderByDocumentBrandCodeAsc(String groupCode);

    List<DocumentBrandEntity> findByGroupCodeAndStatusAndDeletedAtIsNullOrderByDocumentBrandCodeAsc(
            String groupCode,
            DocumentBrandStatus status
    );

    boolean existsByGroupCodeAndDocumentBrandCodeAndDeletedAtIsNull(String groupCode, String documentBrandCode);
}
