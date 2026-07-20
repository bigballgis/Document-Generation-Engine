package com.bank.docgen.documentbrand.persistence;

import com.bank.docgen.documentbrand.domain.DocumentBrandStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LegalEntityRepository extends JpaRepository<LegalEntityEntity, UUID> {

    Optional<LegalEntityEntity> findByGroupCodeAndLegalEntityCodeAndDeletedAtIsNull(
            String groupCode,
            String legalEntityCode
    );

    List<LegalEntityEntity> findByGroupCodeAndDeletedAtIsNullOrderByLegalEntityCodeAsc(String groupCode);

    List<LegalEntityEntity> findByGroupCodeAndStatusAndDeletedAtIsNullOrderByLegalEntityCodeAsc(
            String groupCode,
            DocumentBrandStatus status
    );

    boolean existsByGroupCodeAndLegalEntityCodeAndDeletedAtIsNull(String groupCode, String legalEntityCode);
}
